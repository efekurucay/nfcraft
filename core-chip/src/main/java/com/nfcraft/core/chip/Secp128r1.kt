package com.nfcraft.core.chip

import java.math.BigInteger

/**
 * ECDSA verification over the SECG `secp128r1` curve in pure Kotlin.
 *
 * NXP originality signatures use `secp128r1`, a 128-bit curve that Android's
 * bundled security providers do not expose, so the field and group arithmetic
 * are carried out here directly with [BigInteger]. The curve is defined over
 * the prime field `F_p` by `y^2 = x^3 + a*x + b`, with base point `G` of prime
 * order `n`.
 *
 * [verify] follows the standard ECDSA check with no message hashing
 * (`NONEwithECDSA`): the originality signature is computed over the raw tag UID,
 * which is shorter than the group order, so the UID is used directly as the
 * integer `e`.
 */
internal object Secp128r1 {

    private val P = bi("FFFFFFFDFFFFFFFFFFFFFFFFFFFFFFFF")
    private val A = bi("FFFFFFFDFFFFFFFFFFFFFFFFFFFFFFFC")
    private val B = bi("E87579C11079F43DD824993C2CEE5ED3")
    private val N = bi("FFFFFFFE0000000075A30D1B9038A115")
    private val GX = bi("161FF7528B899B2D0C28607CA52C5B86")
    private val GY = bi("CF5AC8395BAFEB13C02DA292DDED7A83")
    private val G = Point(GX, GY)

    /** Affine point on the curve, or [INFINITY] for the group identity. */
    private class Point(val x: BigInteger?, val y: BigInteger?)

    private val INFINITY = Point(null, null)

    /**
     * Verify an ECDSA signature.
     *
     * @param publicKey uncompressed SEC point (`0x04` || X || Y), 33 bytes.
     * @param message the integer input `e` as big-endian bytes (the raw UID).
     * @param signature `r` || `s`, each 16 bytes big-endian (32 bytes total).
     * @return true when the signature validates against [publicKey].
     */
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size != 33 || publicKey[0].toInt() != 0x04) return false
        if (signature.size != 32) return false

        val qx = BigInteger(1, publicKey.copyOfRange(1, 17))
        val qy = BigInteger(1, publicKey.copyOfRange(17, 33))
        val q = Point(qx, qy)
        if (!isOnCurve(q)) return false

        val r = BigInteger(1, signature.copyOfRange(0, 16))
        val s = BigInteger(1, signature.copyOfRange(16, 32))
        if (r < BigInteger.ONE || r >= N || s < BigInteger.ONE || s >= N) return false

        val e = BigInteger(1, message)
        val w = s.modInverse(N)
        val u1 = e.multiply(w).mod(N)
        val u2 = r.multiply(w).mod(N)

        val point = add(multiply(G, u1), multiply(q, u2))
        val px = point.x ?: return false
        return px.mod(N) == r.mod(N)
    }

    private fun isOnCurve(p: Point): Boolean {
        val x = p.x ?: return false
        val y = p.y ?: return false
        if (x < BigInteger.ZERO || x >= P || y < BigInteger.ZERO || y >= P) return false
        val lhs = y.multiply(y).mod(P)
        val rhs = x.multiply(x).mod(P).multiply(x).add(A.multiply(x)).add(B).mod(P)
        return lhs == rhs
    }

    private fun add(p1: Point, p2: Point): Point {
        if (p1.x == null) return p2
        if (p2.x == null) return p1
        val x1 = p1.x; val y1 = p1.y!!
        val x2 = p2.x; val y2 = p2.y!!
        if (x1 == x2) {
            if (y1.add(y2).mod(P) == BigInteger.ZERO) return INFINITY
            return double(p1)
        }
        val slope = y2.subtract(y1).multiply(x2.subtract(x1).modInverse(P)).mod(P)
        val x3 = slope.multiply(slope).subtract(x1).subtract(x2).mod(P)
        val y3 = slope.multiply(x1.subtract(x3)).subtract(y1).mod(P)
        return Point(x3, y3)
    }

    private fun double(p: Point): Point {
        val x = p.x ?: return INFINITY
        val y = p.y!!
        if (y.signum() == 0) return INFINITY
        val slope = x.multiply(x).multiply(THREE).add(A)
            .multiply(y.multiply(TWO).modInverse(P)).mod(P)
        val x3 = slope.multiply(slope).subtract(x.multiply(TWO)).mod(P)
        val y3 = slope.multiply(x.subtract(x3)).subtract(y).mod(P)
        return Point(x3, y3)
    }

    private fun multiply(point: Point, scalar: BigInteger): Point {
        var result = INFINITY
        var addend = point
        var k = scalar
        while (k.signum() > 0) {
            if (k.testBit(0)) result = add(result, addend)
            addend = double(addend)
            k = k.shiftRight(1)
        }
        return result
    }

    private val TWO = BigInteger.valueOf(2)
    private val THREE = BigInteger.valueOf(3)

    private fun bi(hex: String): BigInteger = BigInteger(hex, 16)
}
