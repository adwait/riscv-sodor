//**************************************************************************
// RISCV Processor
//--------------------------------------------------------------------------

package sodor.stage5

import chisel3._
import sodor.common._

import freechips.rocketchip.config.Parameters
import freechips.rocketchip.tile.CoreInterrupts

class CoreAbstractSignalIO(implicit val conf: SodorCoreParams) extends Bundle {
  val lft_tile_regfile = Output(UInt((32*conf.xprlen).W))
}

class CoreIo(implicit val p: Parameters, val conf: SodorCoreParams) extends Bundle
{
   val ddpath = Flipped(new DebugDPath())
   val dcpath = Flipped(new DebugCPath())
   val imem = new MemPortIo(conf.xprlen)
   val dmem = new MemPortIo(conf.xprlen)
   val interrupt = Input(new CoreInterrupts())
   val hartid = Input(UInt())
   val reset_vector = Input(UInt())

   val sigIO = new CoreAbstractSignalIO
}

class Core()(implicit val p: Parameters, val conf: SodorCoreParams) extends AbstractCore
{
   val io = IO(new CoreIo())
   val c  = Module(new CtlPath())
   val d  = Module(new DatPath())

   c.io.ctl  <> d.io.ctl
   c.io.dat  <> d.io.dat

   io.imem <> c.io.imem
   io.imem <> d.io.imem

   io.dmem <> c.io.dmem
   io.dmem <> d.io.dmem

   d.io.ddpath <> io.ddpath
   c.io.dcpath <> io.dcpath

   d.io.interrupt := io.interrupt
   d.io.hartid := io.hartid
   d.io.reset_vector := io.reset_vector

   io.sigIO.lft_tile_regfile <> d.io.sigIO.lft_tile_regfile
   dontTouch(io.sigIO.lft_tile_regfile)

   val mem_ports = List(io.dmem, io.imem)
   val interrupt = io.interrupt
   val hartid = io.hartid
   val reset_vector = io.reset_vector
}
