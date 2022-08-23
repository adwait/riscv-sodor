//**************************************************************************
// RISCV Processor Register File
//--------------------------------------------------------------------------
//

package sodor.stage5

import chisel3._
import chisel3.util._


import Constants._
import sodor.common._

class RFileAbstractSignalIO(implicit val conf: SodorCoreParams) extends Bundle()
{
   val lft_tile_regfile = Output(UInt((32*conf.xprlen).W))
}

class RFileIo(implicit val conf: SodorCoreParams) extends Bundle()
{
   val rs1_addr = Input(UInt(5.W))
   val rs1_data = Output(UInt(conf.xprlen.W))
   val rs2_addr = Input(UInt(5.W))
   val rs2_data = Output(UInt(conf.xprlen.W))
   val dm_addr = Input(UInt(5.W))
   val dm_rdata = Output(UInt(conf.xprlen.W))
   val dm_wdata = Input(UInt(conf.xprlen.W))
   val dm_en = Input(Bool())

   val waddr    = Input(UInt(5.W))
   val wdata    = Input(UInt(conf.xprlen.W))
   val wen      = Input(Bool())

   val sigIO = new RFileAbstractSignalIO
}

class RegisterFile(implicit val conf: SodorCoreParams) extends Module
{
   val io = IO(new RFileIo())

   val regfile = Mem(32, UInt(conf.xprlen.W))

   io.sigIO.lft_tile_regfile := Cat(regfile(31) , regfile(30) , regfile(29) , regfile(28) , regfile(27) , regfile(26) , regfile(25) , regfile(24) , regfile(23) , regfile(22) , regfile(21) , regfile(20) , regfile(19) , regfile(18) , regfile(17) , regfile(16) , regfile(15) , regfile(14) , regfile(13) , regfile(12) , regfile(11) , regfile(10) , regfile(9) , regfile(8) , regfile(7) , regfile(6) , regfile(5) , regfile(4) , regfile(3) , regfile(2) , regfile(1) , regfile(0))


   when (io.wen && (io.waddr =/= 0.U))
   {
      regfile(io.waddr) := io.wdata
   }

   when (io.dm_en && (io.dm_addr =/= 0.U))
   {
      regfile(io.dm_addr) := io.dm_wdata
   }

   io.rs1_data := Mux((io.rs1_addr =/= 0.U), regfile(io.rs1_addr), 0.U)
   io.rs2_data := Mux((io.rs2_addr =/= 0.U), regfile(io.rs2_addr), 0.U)
   io.dm_rdata := Mux((io.dm_addr =/= 0.U), regfile(io.dm_addr), 0.U)

}
