package model

import util.StreamUtil

data class Vec2Int(var x: Int = 0, var y: Int = 0) {
    companion object {
        @Throws(java.io.IOException::class)
        fun readFrom(stream: java.io.InputStream): Vec2Int {
            val result = Vec2Int()
            result.x = StreamUtil.readInt(stream)
            result.y = StreamUtil.readInt(stream)
            return result
        }
    }
    @Throws(java.io.IOException::class)
    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeInt(stream, x)
        StreamUtil.writeInt(stream, y)
    }
}
