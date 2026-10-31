package com.ymcgps.geopilot

import com.ymcgps.geopilot.util.CoordinateParser
import com.ymcgps.geopilot.util.CoordinateParser.Problem
import com.ymcgps.geopilot.util.CoordinateParser.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateParserTest {

    @Test
    fun parsesDotDecimals() {
        val result = CoordinateParser.parse("41.0082", "28.9784")
        assertTrue(result is Result.Success)
        result as Result.Success
        assertEquals(41.0082, result.latitude, 1e-9)
        assertEquals(28.9784, result.longitude, 1e-9)
    }

    @Test
    fun parsesCommaDecimals() {
        val result = CoordinateParser.parse("41,0082", "28,9784")
        assertTrue(result is Result.Success)
        result as Result.Success
        assertEquals(41.0082, result.latitude, 1e-9)
    }

    @Test
    fun parsesNegativeValues() {
        val result = CoordinateParser.parse("-33.8688", "-151.2093")
        assertTrue(result is Result.Success)
    }

    @Test
    fun rejectsEmptyFields() {
        assertEquals(Result.Failure(Problem.EMPTY_LAT), CoordinateParser.parse("  ", "10"))
        assertEquals(Result.Failure(Problem.EMPTY_LON), CoordinateParser.parse("10", ""))
    }

    @Test
    fun rejectsNonNumbers() {
        assertEquals(Result.Failure(Problem.INVALID_LAT), CoordinateParser.parse("abc", "10"))
        assertEquals(Result.Failure(Problem.INVALID_LON), CoordinateParser.parse("10", "NaN"))
        assertEquals(Result.Failure(Problem.INVALID_LAT), CoordinateParser.parse("Infinity", "10"))
    }

    @Test
    fun rejectsOutOfRange() {
        assertEquals(Result.Failure(Problem.LAT_RANGE), CoordinateParser.parse("90.5", "10"))
        assertEquals(Result.Failure(Problem.LON_RANGE), CoordinateParser.parse("10", "-180.1"))
    }

    @Test
    fun acceptsBoundaries() {
        assertTrue(CoordinateParser.parse("90", "180") is Result.Success)
        assertTrue(CoordinateParser.parse("-90", "-180") is Result.Success)
    }

    @Test
    fun isValidChecksRangeAndFiniteness() {
        assertTrue(CoordinateParser.isValid(0.0, 0.0))
        assertFalse(CoordinateParser.isValid(Double.NaN, 0.0))
        assertFalse(CoordinateParser.isValid(0.0, 181.0))
    }

    @Test
    fun formatsWithDotAndSixDecimals() {
        assertEquals("41.008200", CoordinateParser.format(41.0082))
    }
}
