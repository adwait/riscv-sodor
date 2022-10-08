//**************************************************************************
// RISCV Processor
//--------------------------------------------------------------------------

package sodor.stage3

import chisel3._
import chisel3.util._

import sodor.common._

import freechips.rocketchip.config.Parameters
import freechips.rocketchip.tile.CoreInterrupts

class CoreAbstractSignalIO(implicit val conf: SodorCoreParams) extends Bundle {
  val lft_tile_regfile = Output(UInt((32*conf.xprlen).W))
  
  val lft_tile_regfile_io_rs1_addr = Output(UInt(5.W))
  val lft_tile_regfile_io_rs2_addr = Output(UInt(5.W))
  val lft_tile_regfile_io_rs1_data = Output(UInt(conf.xprlen.W))
  val lft_tile_regfile_io_rs2_data = Output(UInt(conf.xprlen.W))
  
  val lft_tile_imm_itype_sext = Output(UInt(conf.xprlen.W))
  val lft_tile_imm_sbtype_sext = Output(UInt(conf.xprlen.W))
  val lft_tile_imm_stype_sext = Output(UInt(conf.xprlen.W))
  
  val lft_tile_exe_alu_out = Output(UInt(conf.xprlen.W))
  val lft_tile_wb_reg_wbdata = Output(UInt(conf.xprlen.W))
  val lft_tile_exe_reg_wbaddr = Output(UInt(5.W))
  val lft_tile_wb_reg_wbaddr = Output(UInt(5.W))
  
  val lft_tile_wb_reg_inst = Output(UInt(32.W))
  val lft_tile_wb_reg_pc = Output(UInt(32.W))

  val lft_tile_alu_fun = Output(UInt(4.W))
  val lft_tile_mem_fcn = Output(UInt(1.W))
  val lft_tile_mem_typ = Output(UInt(3.W))
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

class Core(implicit val p: Parameters, val conf: SodorCoreParams) extends AbstractCore
{
  val io = IO(new CoreIo())

  val frontend = Module(new FrontEnd())
  val cpath  = Module(new CtlPath())
  val dpath  = Module(new DatPath())

  frontend.io.reset_vector := io.reset_vector
  frontend.io.imem <> io.imem
  frontend.io.cpu <> cpath.io.imem
  frontend.io.cpu <> dpath.io.imem
  frontend.io.cpu.req.valid := cpath.io.imem.req.valid
  frontend.io.cpu.exe_kill := cpath.io.imem.exe_kill

  cpath.io.ctl  <> dpath.io.ctl
  cpath.io.dat  <> dpath.io.dat

  cpath.io.dmem <> io.dmem
  dpath.io.dmem <> io.dmem

  dpath.io.ddpath <> io.ddpath
  cpath.io.dcpath <> io.dcpath

  dpath.io.interrupt := io.interrupt
  dpath.io.hartid := io.hartid

  io.sigIO.lft_tile_regfile <> dpath.io.sigIO.lft_tile_regfile
  io.sigIO.lft_tile_regfile_io_rs1_addr <> dpath.io.sigIO.lft_tile_regfile_io_rs1_addr
  io.sigIO.lft_tile_regfile_io_rs2_addr <> dpath.io.sigIO.lft_tile_regfile_io_rs2_addr
  io.sigIO.lft_tile_regfile_io_rs1_data <> dpath.io.sigIO.lft_tile_regfile_io_rs1_data
  io.sigIO.lft_tile_regfile_io_rs2_data <> dpath.io.sigIO.lft_tile_regfile_io_rs2_data
  io.sigIO.lft_tile_imm_itype_sext <> dpath.io.sigIO.lft_tile_imm_itype_sext
  io.sigIO.lft_tile_imm_sbtype_sext <> dpath.io.sigIO.lft_tile_imm_sbtype_sext
  io.sigIO.lft_tile_imm_stype_sext <> dpath.io.sigIO.lft_tile_imm_stype_sext
  io.sigIO.lft_tile_exe_alu_out <> dpath.io.sigIO.lft_tile_exe_alu_out
  io.sigIO.lft_tile_wb_reg_wbdata <> dpath.io.sigIO.lft_tile_wb_reg_wbdata
  io.sigIO.lft_tile_exe_reg_wbaddr <> dpath.io.sigIO.lft_tile_exe_reg_wbaddr
  io.sigIO.lft_tile_wb_reg_wbaddr <> dpath.io.sigIO.lft_tile_wb_reg_wbaddr
  io.sigIO.lft_tile_wb_reg_inst <> dpath.io.sigIO.lft_tile_wb_reg_inst
  io.sigIO.lft_tile_wb_reg_pc <> dpath.io.sigIO.lft_tile_wb_reg_pc
  io.sigIO.lft_tile_alu_fun <> cpath.io.sigIO.lft_tile_alu_fun
  io.sigIO.lft_tile_mem_fcn <> cpath.io.sigIO.lft_tile_mem_fcn
  io.sigIO.lft_tile_mem_typ <> cpath.io.sigIO.lft_tile_mem_typ

  dontTouch(io.sigIO.lft_tile_regfile)
  dontTouch(io.sigIO.lft_tile_regfile_io_rs1_addr)
  dontTouch(io.sigIO.lft_tile_regfile_io_rs2_addr)
  dontTouch(io.sigIO.lft_tile_regfile_io_rs1_data)
  dontTouch(io.sigIO.lft_tile_regfile_io_rs2_data)
  dontTouch(io.sigIO.lft_tile_imm_itype_sext)
  dontTouch(io.sigIO.lft_tile_imm_sbtype_sext)
  dontTouch(io.sigIO.lft_tile_imm_stype_sext)
  dontTouch(io.sigIO.lft_tile_exe_alu_out)
  dontTouch(io.sigIO.lft_tile_wb_reg_wbdata)
  dontTouch(io.sigIO.lft_tile_exe_reg_wbaddr)
  dontTouch(io.sigIO.lft_tile_wb_reg_wbaddr)
  dontTouch(io.sigIO.lft_tile_wb_reg_inst)
  dontTouch(io.sigIO.lft_tile_wb_reg_pc)
  dontTouch(io.sigIO.lft_tile_alu_fun)
  dontTouch(io.sigIO.lft_tile_mem_fcn)
  dontTouch(io.sigIO.lft_tile_mem_typ)

  val mem_ports = List(io.dmem, io.imem)
  val interrupt = io.interrupt
  val hartid = io.hartid
  val reset_vector = io.reset_vector
}
