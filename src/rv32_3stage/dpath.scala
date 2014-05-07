//**************************************************************************
// RISCV Processor 3-Stage Datapath
//--------------------------------------------------------------------------
//
// Christopher Celio
// 2013 Jun 29
//
// This concerns the "backend" datapath for the Z-Scale 3-stage processor.  The
// frontend is separated out (since the front-end is relatively decoupled, and
// because the designer may wish to swap out different front-ends for different
// performance/area tradeoffs).
//
// Thus, this file covers the Execute and Writeback stages on the 3-stage.

package Sodor
{

import Chisel._
import Node._

import Constants._
import Common._
import Common.Constants._

class DatToCtlIo extends Bundle() 
{
   val br_eq  = Bool(OUTPUT)
   val br_lt  = Bool(OUTPUT)
   val br_ltu = Bool(OUTPUT)
   val status = new Status().asOutput
}

class DpathIo(implicit conf: SodorConfiguration) extends Bundle() 
{
   val host  = new HTIFIO()
   val imem = new FrontEndCpuIO().flip()
   val dmem = new MemPortIo(conf.xprlen)
   val ctl  = new CtrlSignals().asInput
   val dat  = new DatToCtlIo()
}

class DatPath(implicit conf: SodorConfiguration) extends Module 
{
   val io = new DpathIo()


   //**********************************
   // Pipeline State Registers

   val wb_reg_ctrl     = Reg(outType = new CtrlSignals)
   val wb_reg_alu      = Reg(outType = Bits(width=conf.xprlen))
   val wb_reg_rs1_addr = Reg(outType = UInt(width=log2Up(32))) // needed for CSR
   val wb_reg_wbaddr   = Reg(outType = UInt(width=log2Up(32)))
   
   val wb_hazard_stall = Bool() // hazard detected, stall in IF/EXE required

   //**********************************
   // Instruction Fetch Stage
   val exe_brjmp_target    = UInt()
   val exe_jump_reg_target = UInt()
   val exception_target    = UInt()
   val exe_reg_pc_plus4        = UInt()

   io.imem.resp.ready := !wb_hazard_stall // stall IF if we detect a WB->EXE hazard

   val if_pc_next = Mux(io.ctl.pc_sel === PC_4,     exe_reg_pc_plus4,
                    Mux(io.ctl.pc_sel === PC_EXC,   exception_target,
                    Mux(io.ctl.pc_sel === PC_JR,    exe_jump_reg_target,
                                                    exe_brjmp_target))) // PC_BR or PC_J
                                                    

   io.imem.req.bits.pc := if_pc_next

   
   //**********************************
   // Execute Stage
   val exe_valid = io.imem.resp.valid
   val exe_inst  = io.imem.resp.bits.inst
   val exe_reg_pc = Reg(init=UInt(0, width=conf.xprlen))
   when(exe_valid) {
     exe_reg_pc := if_pc_next
   }
   
   // Decode
   val exe_rs1_addr = exe_inst(19, 15).toUInt
   val exe_rs2_addr = exe_inst(24, 20).toUInt
   val exe_wbaddr   = exe_inst(11, 7).toUInt
                       
   val wb_wbdata    = Bits(width = conf.xprlen)
 
   exe_reg_pc_plus4    := exe_reg_pc + UInt(4)

   // Hazard Stall Logic 
   wb_hazard_stall := ((wb_reg_wbaddr === exe_rs1_addr) && (exe_rs1_addr != UInt(0)) && wb_reg_ctrl.rf_wen && !wb_reg_ctrl.bypassable) || 
                      ((wb_reg_wbaddr === exe_rs2_addr) && (exe_rs2_addr != UInt(0)) && wb_reg_ctrl.rf_wen && !wb_reg_ctrl.bypassable)


   // Register File
   val regfile = Mem(Bits(width = conf.xprlen), 32)

   when (wb_reg_ctrl.rf_wen && (wb_reg_wbaddr != UInt(0)))
   {
      regfile(wb_reg_wbaddr) := wb_wbdata
   }

   val rf_rs1_data = Mux((exe_rs1_addr != UInt(0)), regfile(exe_rs1_addr), UInt(0, conf.xprlen))
   val rf_rs2_data = Mux((exe_rs2_addr != UInt(0)), regfile(exe_rs2_addr), UInt(0, conf.xprlen))
   
   
   // immediates
   val imm_i = exe_inst(31, 20) 
   val imm_s = Cat(exe_inst(31, 25), exe_inst(11,7))
   val imm_b = Cat(exe_inst(31), exe_inst(7), exe_inst(30,25), exe_inst(11,8))
   val imm_u = exe_inst(31, 12)
   val imm_j = Cat(exe_inst(31), exe_inst(19,12), exe_inst(20), exe_inst(30,21))

   val zimm = Cat(Fill(UInt(0), 27), exe_inst(19,15))

   // sign-extend immediates
   val imm_i_sext = Cat(Fill(imm_i(11), 20), imm_i)
   val imm_s_sext = Cat(Fill(imm_s(11), 20), imm_s)
   val imm_b_sext = Cat(Fill(imm_b(11), 19), imm_b, UInt(0))
   val imm_u_sext = Cat(imm_u, Fill(UInt(0), 12))
   val imm_j_sext = Cat(Fill(imm_j(19), 11), imm_j, UInt(0))
 
   
   // Bypass Muxes
   // bypass early for branch condition checking, and to prevent needing 3 bypass muxes
   val exe_rs1_data = MuxCase(rf_rs1_data, Array(
                           ((wb_reg_wbaddr === exe_rs1_addr) && (exe_rs1_addr != UInt(0)) && wb_reg_ctrl.rf_wen && wb_reg_ctrl.bypassable) -> wb_reg_alu)
                        )
   val exe_rs2_data = MuxCase(rf_rs2_data, Array(
                           ((wb_reg_wbaddr === exe_rs2_addr) && (exe_rs2_addr != UInt(0)) && wb_reg_ctrl.rf_wen && wb_reg_ctrl.bypassable) -> wb_reg_alu)
                        )
   

   // Operand Muxes
   val exe_alu_op1 = MuxCase(UInt(0), Array(
               (io.ctl.op1_sel === OP1_RS1)  -> exe_rs1_data,
               (io.ctl.op1_sel === OP1_ZIMM) -> zimm,
               (io.ctl.op1_sel === OP1_PC)   -> exe_reg_pc
               )).toUInt
   
   val exe_alu_op2 = MuxCase(UInt(0), Array(
               (io.ctl.op2_sel === OP2_RS2) -> exe_rs2_data,
               (io.ctl.op2_sel === OP2_IMI) -> imm_i_sext,
               (io.ctl.op2_sel === OP2_IMB) -> imm_b_sext,
               (io.ctl.op2_sel === OP2_IMU) -> imm_u_sext,
               (io.ctl.op2_sel === OP2_IMS) -> imm_s_sext,
               (io.ctl.op2_sel === OP2_IMJ) -> imm_j_sext
              )).toUInt
  
        
   // ALU
   val alu = Module(new ALU())

      alu.io.in1 := exe_alu_op1
      alu.io.in2 := exe_alu_op2
      alu.io.fn  := io.ctl.alu_fun

   val exe_alu_out = alu.io.out

   // Branch/Jump Target Calculation
   val imm_brjmp = Mux(io.ctl.brjmp_sel, imm_j_sext, imm_b_sext)
   exe_brjmp_target := exe_reg_pc + Cat(imm_brjmp(conf.xprlen-2,0), UInt(0,1)).toUInt
   exe_jump_reg_target := alu.io.adder_out 


   // datapath to controlpath outputs
   io.dat.br_eq  := (exe_rs1_data === exe_rs2_data)
   io.dat.br_lt  := (exe_rs1_data.toSInt < exe_rs2_data.toSInt) 
   io.dat.br_ltu := (exe_rs1_data.toUInt < exe_rs2_data.toUInt)
                                  

   // execute to wb registers
   wb_reg_ctrl :=  io.ctl
   when (wb_hazard_stall || io.ctl.exe_kill)
   {
      wb_reg_ctrl.rf_wen    := Bool(false)
      wb_reg_ctrl.csr_cmd   := CSR.N
      wb_reg_ctrl.dmem_val  := Bool(false)
      wb_reg_ctrl.exception := Bool(false)
      wb_reg_ctrl.sret      := Bool(false)
   }

   wb_reg_alu := exe_alu_out
   wb_reg_rs1_addr := exe_rs1_addr
   wb_reg_wbaddr := exe_wbaddr
     
   
   // datapath to data memory outputs
   io.dmem.req.valid     := io.ctl.dmem_val
   io.dmem.req.bits.fcn  := io.ctl.dmem_fcn
   io.dmem.req.bits.typ  := io.ctl.dmem_typ
   io.dmem.req.bits.addr := exe_alu_out
   io.dmem.req.bits.data := exe_rs2_data
                                 
   //**********************************
   // Writeback Stage
   
   // Control Status Registers
   val csr = Module(new CSRFile())
   csr.io.host <> io.host
   csr.io.rw.addr  := exe_alu_op2(11,0)
   csr.io.rw.wdata := exe_alu_op1
   csr.io.rw.cmd   := io.ctl.csr_cmd
   val csr_out = csr.io.rw.rdata

   csr.io.exception := io.ctl.exception
   io.dat.status := csr.io.status
   csr.io.cause  := io.ctl.exc_cause
   csr.io.sret   := io.ctl.sret
   csr.io.pc     := exe_reg_pc
   exception_target := csr.io.evec


   // WB Mux                                                                   
   // Note: I'm relying on the fact that the EXE stage is holding the instruction behind our JR
   // assumes we always predict PC+4, and we don't clear the "mispredicted" PC when we jump
   wb_wbdata := MuxCase(wb_reg_alu, Array(
                  (wb_reg_ctrl.wb_sel === WB_ALU) -> wb_reg_alu,
                  (wb_reg_ctrl.wb_sel === WB_MEM) -> io.dmem.resp.bits.data, 
                  (wb_reg_ctrl.wb_sel === WB_PC4) -> exe_reg_pc_plus4,
                  (wb_reg_ctrl.wb_sel === WB_CSR) -> csr_out
                  )).toSInt()
                                
   
   //**********************************
   // Printout

   val tsc_reg = Reg(init=UInt(0, conf.xprlen))
   tsc_reg := tsc_reg + UInt(1)

   printf("Cyc=%d %s PC=(0x%x,0x%x,0x%x) [%s,%s,%s] Wb: %s %s %s Op1=[0x%x] Op2=[0x%x] W[%s,%d= 0x%x] [%s,%d]\n"
      , tsc_reg(23,0)
      , Mux(wb_hazard_stall, Str("HAZ"), Str("   "))
      , io.imem.debug.if_pc(19,0)
      , exe_reg_pc(19,0)
      , Mux(Reg(next=wb_hazard_stall), UInt(0), Reg(next=exe_reg_pc)(19,0))
      , Disassemble(io.imem.debug.if_inst, true)
      , Disassemble(exe_inst, true)
      , Mux(Reg(next=wb_hazard_stall || io.ctl.exe_kill), Disassemble(BUBBLE, true), Disassemble(Reg(next=exe_inst), true))
      , Mux(Reg(next=wb_hazard_stall || io.ctl.exe_kill), Disassemble(BUBBLE), Disassemble(Reg(next=exe_inst)))
      , Mux(wb_hazard_stall, Str("HAZ"), Str("   "))
      , Mux(io.ctl.pc_sel  === UInt(1), Str("Br/J"),
        Mux(io.ctl.pc_sel === UInt(2), Str(" JR "),
        Mux(io.ctl.pc_sel === UInt(3), Str("XPCT"),
        Mux(io.ctl.pc_sel === UInt(0), Str("   "), Str(" ?? ")))))
      , exe_alu_op1
      , exe_alu_op2
      , Mux(wb_reg_ctrl.rf_wen, Str("W"), Str("_"))
      , wb_reg_wbaddr
      , wb_wbdata
      , Mux(io.ctl.exception, Str("E"), Str("_"))
      , io.ctl.exc_cause
//      , Mux(io.ctl.dmem_val, Str("V"), Str("_"))
//      , io.dmem.req.bits.data
      )

   //**********************************
   // Handle Reset

   when (this.reset)
   {
      wb_reg_ctrl.rf_wen    := Bool(false)
      wb_reg_ctrl.csr_cmd   := CSR.N
      wb_reg_ctrl.dmem_val  := Bool(false)
      wb_reg_ctrl.exception := Bool(false)
      wb_reg_ctrl.sret      := Bool(false)
   }
 
}

 
}
