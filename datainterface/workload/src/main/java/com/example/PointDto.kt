package com.example

import com.example.containers.Foo

data class Point(override val x: Int, override val y: Int): PointDataInterface
data class Point3D(override val x: Int, override val y: Int, val z: Int): PointDataInterface
data class Point3DDto(override val x: Int, override val y: Int, val z: Int): PointDataInterface
data class Point3DStrange(override val x: Int, override val y: Int, val z: Foo): PointDataInterface
