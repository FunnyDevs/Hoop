package app.hoop.domain.hoop

import app.hoop.domain.assertSuccess
import app.hoop.domain.geo.GeoPoint
import app.hoop.domain.geo.Radius

fun testHome() = Hoop.Home(HoopId("home"), Radius.Default)
fun testExtra(id: String) = Hoop.Extra(HoopId(id), "Zona $id", GeoPoint.create(45.0, 9.0).assertSuccess(), Radius.Default)
