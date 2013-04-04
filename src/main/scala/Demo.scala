package demo

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.Implicits._

case class TiledLayer(raster:Raster,tileSums:Map[RasterExtent,Long])

object Main {
  def main(args: Array[String]):Unit = {
      val s = Server("demo", Catalog.fromPath("/home/rob/osgeo/gt/data/catalog.json"))
      try {
        println("Starting test...")
        val startNanos = System.nanoTime()
        val names = s.catalog.stores("TMEAN").getNames.toList
        val count = names.length
        
        // limit should be number of rasters that can fit in memory at a time
        val limit = 30
        
        val loadOps = names.map { n => io.LoadRaster(n) }
        
        val dividedOps:Seq[Op[Raster]] = loadOps.map { rOp => 
          local.Divide(rOp, count) 
        }
        
        val firstRaster:Op[Raster] = dividedOps.head
 
        val groups = dividedOps.tail
                               .grouped(limit)
                               .map { _.toArray }
                               .map(local.AddRasters( _:_* ))
        val sum = local.AddArray(logic.CollectArray( groups.toArray ))
                        
        s.getResult(sum) match {
          case process.Complete(r,h) =>
             val endNanos = System.nanoTime()
             val diff = endNanos - startNanos
             println(s"Measure time: ${diff/1000000} ms")
          case process.Error(message,failure) =>
            sys.error("Didn't work")
        }
      } finally {
        s.shutdown()
      }
  }
}
