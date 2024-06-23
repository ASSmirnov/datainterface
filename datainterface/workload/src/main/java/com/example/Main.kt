package com.example

import com.example.containers.Foo
import com.github.assmirnov.datainterface.from

fun main() {
    val point = Point3D(x=12, y=100, z=23)
    val p2: Point = from(point)
    println(p2)
    val p3: Point3D = from(point, z=23)
    val p4: Point3DStrange = from(point, z=Foo(i=543))
    println(p3)
    println(p4)
}
