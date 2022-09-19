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

   val lft_tile_regfile_io_rs1_addr = Output(UInt(5.W))
   val lft_tile_regfile_io_rs2_addr = Output(UInt(5.W))
   val lft_tile_regfile_io_rs1_data = Output(UInt(conf.xprlen.W))
   val lft_tile_regfile_io_rs2_data = Output(UInt(conf.xprlen.W))
   val lft_tile_wb_reg_wbdata = Output(UInt(conf.xprlen.W))
   val lft_tile_exe_alu_out = Output(UInt(conf.xprlen.W))
   val lft_tile_imm_itype_sext = Output(UInt(conf.xprlen.W))
   val lft_tile_imm_sbtype_sext = Output(UInt(conf.xprlen.W))
   val lft_tile_wb_reg_wbaddr = Output(UInt(5.W))

   val lft_tile_dec_wbaddr = Output(UInt(5.W))
   val lft_tile_exe_reg_wbaddr = Output(UInt(5.W))
   val lft_tile_mem_reg_wbaddr = Output(UInt(5.W))
   val lft_tile_mem_reg_alu_out = Output(UInt(32.W))
   
   val lft_tile_dec_reg_inst = Output(UInt(32.W))
   val lft_tile_exe_reg_inst = Output(UInt(32.W))
   val lft_tile_mem_reg_inst = Output(UInt(32.W))
   val lft_tile_if_reg_pc = Output(UInt(32.W))
   val lft_tile_dec_reg_pc = Output(UInt(32.W))
   val lft_tile_exe_reg_pc = Output(UInt(32.W))
   val lft_tile_mem_reg_pc = Output(UInt(32.W))

   val lft_tile_alu_fun = Output(UInt(4.W))

   val lft_tile_lb_table = Output(new LBEntry())
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
   io.sigIO.lft_tile_exe_alu_out <> d.io.sigIO.lft_tile_exe_alu_out
   io.sigIO.lft_tile_imm_itype_sext <> d.io.sigIO.lft_tile_imm_itype_sext
   io.sigIO.lft_tile_imm_sbtype_sext <> d.io.sigIO.lft_tile_imm_sbtype_sext
   io.sigIO.lft_tile_regfile_io_rs1_addr <> d.io.sigIO.lft_tile_regfile_io_rs1_addr
   io.sigIO.lft_tile_regfile_io_rs2_addr <> d.io.sigIO.lft_tile_regfile_io_rs2_addr
   io.sigIO.lft_tile_regfile_io_rs1_data <> d.io.sigIO.lft_tile_regfile_io_rs1_data
   io.sigIO.lft_tile_regfile_io_rs2_data <> d.io.sigIO.lft_tile_regfile_io_rs2_data
   io.sigIO.lft_tile_wb_reg_wbdata <> d.io.sigIO.lft_tile_wb_reg_wbdata
   io.sigIO.lft_tile_wb_reg_wbaddr <> d.io.sigIO.lft_tile_wb_reg_wbaddr

   io.sigIO.lft_tile_dec_wbaddr <> d.io.sigIO.lft_tile_dec_wbaddr
   io.sigIO.lft_tile_exe_reg_wbaddr <> d.io.sigIO.lft_tile_exe_reg_wbaddr
   io.sigIO.lft_tile_mem_reg_wbaddr <> d.io.sigIO.lft_tile_mem_reg_wbaddr

   io.sigIO.lft_tile_dec_reg_inst <> d.io.sigIO.lft_tile_dec_reg_inst
   io.sigIO.lft_tile_exe_reg_inst <> d.io.sigIO.lft_tile_exe_reg_inst
   io.sigIO.lft_tile_mem_reg_inst <> d.io.sigIO.lft_tile_mem_reg_inst
   io.sigIO.lft_tile_mem_reg_alu_out <> d.io.sigIO.lft_tile_mem_reg_alu_out
   io.sigIO.lft_tile_if_reg_pc <> d.io.sigIO.lft_tile_if_reg_pc
   io.sigIO.lft_tile_dec_reg_pc <> d.io.sigIO.lft_tile_dec_reg_pc
   io.sigIO.lft_tile_exe_reg_pc <> d.io.sigIO.lft_tile_exe_reg_pc
   io.sigIO.lft_tile_mem_reg_pc <> d.io.sigIO.lft_tile_mem_reg_pc

   io.sigIO.lft_tile_alu_fun <> c.io.sigIO.lft_tile_alu_fun
   io.sigIO.lft_tile_lb_table <> d.io.sigIO.lft_tile_lb_table

   dontTouch(io.sigIO.lft_tile_regfile)
   dontTouch(io.sigIO.lft_tile_exe_alu_out)
   dontTouch(io.sigIO.lft_tile_imm_itype_sext)
   dontTouch(io.sigIO.lft_tile_regfile_io_rs1_addr)
   dontTouch(io.sigIO.lft_tile_regfile_io_rs2_addr)
   dontTouch(io.sigIO.lft_tile_regfile_io_rs1_data)
   dontTouch(io.sigIO.lft_tile_regfile_io_rs2_data)
   dontTouch(io.sigIO.lft_tile_wb_reg_wbdata)
   dontTouch(io.sigIO.lft_tile_wb_reg_wbaddr)
   dontTouch(io.sigIO.lft_tile_dec_reg_inst)
   dontTouch(io.sigIO.lft_tile_exe_reg_inst)
   dontTouch(io.sigIO.lft_tile_mem_reg_inst)
   dontTouch(io.sigIO.lft_tile_mem_reg_alu_out)
   dontTouch(io.sigIO.lft_tile_if_reg_pc)
   dontTouch(io.sigIO.lft_tile_dec_reg_pc)
   dontTouch(io.sigIO.lft_tile_exe_reg_pc)
   dontTouch(io.sigIO.lft_tile_mem_reg_pc)
   dontTouch(io.sigIO.lft_tile_lb_table)

   dontTouch(io.sigIO.lft_tile_dec_wbaddr)
   dontTouch(io.sigIO.lft_tile_exe_reg_wbaddr)
   dontTouch(io.sigIO.lft_tile_mem_reg_wbaddr)
   dontTouch(io.sigIO.lft_tile_imm_sbtype_sext)
   dontTouch(io.sigIO.lft_tile_alu_fun)

   val mem_ports = List(io.dmem, io.imem)
   val interrupt = io.interrupt
   val hartid = io.hartid
   val reset_vector = io.reset_vector
}
