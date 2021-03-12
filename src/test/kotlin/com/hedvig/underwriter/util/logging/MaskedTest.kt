package com.hedvig.underwriter.util.logging

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.hedvig.underwriter.model.ProductType
import org.junit.Test
import java.math.BigDecimal

class MaskedTest {

    @Test
    fun testPlainPojo() {
        data class Data(
            val a: String,
            val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        val o = Data("sdf", 123, true, 12.345, null)

        assertThat(o.toMaskedString()).isEqualTo(o.toString())
    }

    @Test
    fun testPojoList() {
        data class Data(
            val a: String,
            val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        val o = listOf(Data("sdf", 123, true, 12.345, null), Data("asfd", 54, true, 7.345, "a"))

        assertThat(o.toMaskedString()).isEqualTo(o.toString())
    }

    @Test
    fun testPojoSet() {
        data class Data(
            val a: String,
            val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        val o = setOf(Data("sdf", 123, true, 12.345, null), Data("asfd", 54, true, 7.345, "a"))

        assertThat(o.toMaskedString()).isEqualTo(o.toString())
    }

    @Test
    fun testPojoMap() {
        data class Data(
            val a: String,
            val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        val o = mapOf(1 to Data("sdf", 123, true, 12.345, null), 3 to Data("asfd", 54, true, 7.345, "a"))

        assertThat(o.toMaskedString()).isEqualTo(o.toString())

        assertThat((o as Any).toMaskedString()).isEqualTo(o.toString())
    }

    @Test
    fun testNestedPojo() {
        data class NestedData(
            val a: String,
            val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        data class Data(
            val a: String,
            val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?,
            val f: NestedData
        )

        val o = Data("sdf", 123, true, 12.345, null, NestedData("asdds", 134, false, 34.121, "asda"))

        assertThat(o.toMaskedString()).isEqualTo(o.toString())
    }

    @Test
    fun testPlainPojoWithMasked() {
        data class Data(
            @Masked val a: String,
            @Masked val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        val o = Data("sdf", 123, true, 12.345, null)

        assertThat(o.toMaskedString()).isEqualTo("Data(a=***, b=***, c=true, d=12.345, e=null)")
    }

    @Test
    fun testPlainPojoWithEnum() {
        data class Data(
            val a: ProductType
        )

        val o = Data(ProductType.HOME_CONTENT)

        assertThat(o.toMaskedString()).isEqualTo("Data(a=HOME_CONTENT)")
    }

    @Test
    fun testNestedPojoWithMasked() {
        data class NestedData(
            @Masked val a: String,
            @Masked val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        data class Data(
            val a: String,
            val b: Int,
            @Masked val c: Boolean,
            @Masked val d: Double,
            @Masked val e: String?,
            val f: NestedData
        )

        val o = Data("sdf", 123, true, 12.345, null, NestedData("asdds", 134, false, 34.121, "asda"))

        assertThat(o.toMaskedString()).isEqualTo("Data(a=sdf, b=123, c=***, d=***, e=null, f=NestedData(a=***, b=***, c=false, d=34.121, e=asda))")
    }

    @Test
    fun testMapWithNestedPojoWithPii() {
        data class NestedData(
            @Masked val a: String,
            @Masked val b: Int,
            val c: Boolean,
            val d: Double,
            val e: String?
        )

        data class Data(
            val a: String,
            val b: Int,
            @Masked val c: Boolean,
            @Masked val d: Double,
            @Masked val e: String?,
            val f: NestedData
        )

        val o = mapOf(1 to Data("sdf", 123, true, 12.345, null, NestedData("asdds", 134, false, 34.121, "asda")), 2 to null)

        assertThat(o.toMaskedString()).isEqualTo("{1=Data(a=sdf, b=123, c=***, d=***, e=null, f=NestedData(a=***, b=***, c=false, d=34.121, e=asda)), 2=null}")
    }

    @Test
    fun testNestedPojoMapWithListsAndMaps() {
        data class Item(
            @Masked val a: String,
            val b: String
        )

        data class Data(
            val a: List<*>,
            val b: Map<String, *>,
            val c: Item
        )

        val o = mapOf(
            1 to Data(
                a = listOf("1", 1, 2.0, Item("masked", "banan")),
                b = mapOf("1" to 1, "2" to Item("masked", "citron")),
                c = Item("masked", "banan")
            ),
            2 to "sadf")

        assertThat(o.toMaskedString()).isEqualTo(o.toString().replace("masked", "***"))
    }

    @Test
    fun testDowncastedPojoMapWithMaskedVals() {
        data class Data(
            @Masked val a: String
        )

        val o = mapOf(1 to Data("masked"))

        assertThat(o.toMaskedString()).isEqualTo("{1=Data(a=***)}")
        assertThat((o as Any).toMaskedString()).isEqualTo("{1=Data(a=***)}")
    }

    @Test
    fun testInheritance() {
        open class Parent(
            @Masked val a: String,
            val b: String
        )

        class Child(
            a: String,
            b: String,
            @Masked val c: String,
            val d: String
        ) : Parent(a, b)

        val a = Child("masked", "2", "masked", "4")

        assertThat(a.toMaskedString()).isEqualTo("Child(c=***, d=4, a=***, b=2)")
    }

    @Test
    fun testNull() {

        val a: String? = null

        assertThat(a.toMaskedString()).isEqualTo("null")
    }

    @Test
    fun testKotlinBuildIns() {

        val a = "dafadf"
        assertThat(a.toMaskedString()).isEqualTo(a)

        val b = 123.341
        assertThat(b.toMaskedString()).isEqualTo(b.toString())

        val c = false
        assertThat(c.toMaskedString()).isEqualTo(c.toString())

        val d = BigDecimal(12)
        assertThat(d.toMaskedString()).isEqualTo(d.toString())
    }
}
