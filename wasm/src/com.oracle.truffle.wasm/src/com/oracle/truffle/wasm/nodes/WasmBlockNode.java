/*
 * Copyright (c) 2019, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.wasm.nodes;

import static com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import static com.oracle.truffle.wasm.Assert.format;
import static com.oracle.truffle.wasm.WasmTracing.trace;
import static com.oracle.truffle.wasm.constants.Instructions.BLOCK;
import static com.oracle.truffle.wasm.constants.Instructions.BR;
import static com.oracle.truffle.wasm.constants.Instructions.BR_IF;
import static com.oracle.truffle.wasm.constants.Instructions.BR_TABLE;
import static com.oracle.truffle.wasm.constants.Instructions.CALL;
import static com.oracle.truffle.wasm.constants.Instructions.CALL_INDIRECT;
import static com.oracle.truffle.wasm.constants.Instructions.DROP;
import static com.oracle.truffle.wasm.constants.Instructions.ELSE;
import static com.oracle.truffle.wasm.constants.Instructions.END;
import static com.oracle.truffle.wasm.constants.Instructions.F32_ABS;
import static com.oracle.truffle.wasm.constants.Instructions.F32_ADD;
import static com.oracle.truffle.wasm.constants.Instructions.F32_CEIL;
import static com.oracle.truffle.wasm.constants.Instructions.F32_CONST;
import static com.oracle.truffle.wasm.constants.Instructions.F32_CONVERT_I32_S;
import static com.oracle.truffle.wasm.constants.Instructions.F32_CONVERT_I32_U;
import static com.oracle.truffle.wasm.constants.Instructions.F32_CONVERT_I64_S;
import static com.oracle.truffle.wasm.constants.Instructions.F32_CONVERT_I64_U;
import static com.oracle.truffle.wasm.constants.Instructions.F32_COPYSIGN;
import static com.oracle.truffle.wasm.constants.Instructions.F32_DEMOTE_F64;
import static com.oracle.truffle.wasm.constants.Instructions.F32_DIV;
import static com.oracle.truffle.wasm.constants.Instructions.F32_EQ;
import static com.oracle.truffle.wasm.constants.Instructions.F32_FLOOR;
import static com.oracle.truffle.wasm.constants.Instructions.F32_GE;
import static com.oracle.truffle.wasm.constants.Instructions.F32_GT;
import static com.oracle.truffle.wasm.constants.Instructions.F32_LE;
import static com.oracle.truffle.wasm.constants.Instructions.F32_LOAD;
import static com.oracle.truffle.wasm.constants.Instructions.F32_LT;
import static com.oracle.truffle.wasm.constants.Instructions.F32_MAX;
import static com.oracle.truffle.wasm.constants.Instructions.F32_MIN;
import static com.oracle.truffle.wasm.constants.Instructions.F32_MUL;
import static com.oracle.truffle.wasm.constants.Instructions.F32_NE;
import static com.oracle.truffle.wasm.constants.Instructions.F32_NEAREST;
import static com.oracle.truffle.wasm.constants.Instructions.F32_NEG;
import static com.oracle.truffle.wasm.constants.Instructions.F32_REINTERPRET_I32;
import static com.oracle.truffle.wasm.constants.Instructions.F32_SQRT;
import static com.oracle.truffle.wasm.constants.Instructions.F32_STORE;
import static com.oracle.truffle.wasm.constants.Instructions.F32_SUB;
import static com.oracle.truffle.wasm.constants.Instructions.F32_TRUNC;
import static com.oracle.truffle.wasm.constants.Instructions.F64_ABS;
import static com.oracle.truffle.wasm.constants.Instructions.F64_ADD;
import static com.oracle.truffle.wasm.constants.Instructions.F64_CEIL;
import static com.oracle.truffle.wasm.constants.Instructions.F64_CONST;
import static com.oracle.truffle.wasm.constants.Instructions.F64_CONVERT_I32_S;
import static com.oracle.truffle.wasm.constants.Instructions.F64_CONVERT_I32_U;
import static com.oracle.truffle.wasm.constants.Instructions.F64_CONVERT_I64_S;
import static com.oracle.truffle.wasm.constants.Instructions.F64_CONVERT_I64_U;
import static com.oracle.truffle.wasm.constants.Instructions.F64_COPYSIGN;
import static com.oracle.truffle.wasm.constants.Instructions.F64_DIV;
import static com.oracle.truffle.wasm.constants.Instructions.F64_EQ;
import static com.oracle.truffle.wasm.constants.Instructions.F64_FLOOR;
import static com.oracle.truffle.wasm.constants.Instructions.F64_GE;
import static com.oracle.truffle.wasm.constants.Instructions.F64_GT;
import static com.oracle.truffle.wasm.constants.Instructions.F64_LE;
import static com.oracle.truffle.wasm.constants.Instructions.F64_LOAD;
import static com.oracle.truffle.wasm.constants.Instructions.F64_LT;
import static com.oracle.truffle.wasm.constants.Instructions.F64_MAX;
import static com.oracle.truffle.wasm.constants.Instructions.F64_MIN;
import static com.oracle.truffle.wasm.constants.Instructions.F64_MUL;
import static com.oracle.truffle.wasm.constants.Instructions.F64_NE;
import static com.oracle.truffle.wasm.constants.Instructions.F64_NEAREST;
import static com.oracle.truffle.wasm.constants.Instructions.F64_NEG;
import static com.oracle.truffle.wasm.constants.Instructions.F64_PROMOTE_F32;
import static com.oracle.truffle.wasm.constants.Instructions.F64_REINTERPRET_I64;
import static com.oracle.truffle.wasm.constants.Instructions.F64_SQRT;
import static com.oracle.truffle.wasm.constants.Instructions.F64_STORE;
import static com.oracle.truffle.wasm.constants.Instructions.F64_SUB;
import static com.oracle.truffle.wasm.constants.Instructions.F64_TRUNC;
import static com.oracle.truffle.wasm.constants.Instructions.GLOBAL_GET;
import static com.oracle.truffle.wasm.constants.Instructions.GLOBAL_SET;
import static com.oracle.truffle.wasm.constants.Instructions.I32_ADD;
import static com.oracle.truffle.wasm.constants.Instructions.I32_AND;
import static com.oracle.truffle.wasm.constants.Instructions.I32_CLZ;
import static com.oracle.truffle.wasm.constants.Instructions.I32_CONST;
import static com.oracle.truffle.wasm.constants.Instructions.I32_CTZ;
import static com.oracle.truffle.wasm.constants.Instructions.I32_DIV_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_DIV_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_EQ;
import static com.oracle.truffle.wasm.constants.Instructions.I32_EQZ;
import static com.oracle.truffle.wasm.constants.Instructions.I32_GE_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_GE_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_GT_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_GT_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LE_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LE_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LOAD;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LOAD16_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LOAD16_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LOAD8_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LOAD8_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LT_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_LT_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_MUL;
import static com.oracle.truffle.wasm.constants.Instructions.I32_NE;
import static com.oracle.truffle.wasm.constants.Instructions.I32_OR;
import static com.oracle.truffle.wasm.constants.Instructions.I32_POPCNT;
import static com.oracle.truffle.wasm.constants.Instructions.I32_REINTERPRET_F32;
import static com.oracle.truffle.wasm.constants.Instructions.I32_REM_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_REM_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_ROTL;
import static com.oracle.truffle.wasm.constants.Instructions.I32_ROTR;
import static com.oracle.truffle.wasm.constants.Instructions.I32_SHL;
import static com.oracle.truffle.wasm.constants.Instructions.I32_SHR_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_SHR_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_STORE;
import static com.oracle.truffle.wasm.constants.Instructions.I32_STORE_16;
import static com.oracle.truffle.wasm.constants.Instructions.I32_STORE_8;
import static com.oracle.truffle.wasm.constants.Instructions.I32_SUB;
import static com.oracle.truffle.wasm.constants.Instructions.I32_TRUNC_F32_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_TRUNC_F32_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_TRUNC_F64_S;
import static com.oracle.truffle.wasm.constants.Instructions.I32_TRUNC_F64_U;
import static com.oracle.truffle.wasm.constants.Instructions.I32_WRAP_I64;
import static com.oracle.truffle.wasm.constants.Instructions.I32_XOR;
import static com.oracle.truffle.wasm.constants.Instructions.I64_ADD;
import static com.oracle.truffle.wasm.constants.Instructions.I64_AND;
import static com.oracle.truffle.wasm.constants.Instructions.I64_CLZ;
import static com.oracle.truffle.wasm.constants.Instructions.I64_CONST;
import static com.oracle.truffle.wasm.constants.Instructions.I64_CTZ;
import static com.oracle.truffle.wasm.constants.Instructions.I64_DIV_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_DIV_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_EQ;
import static com.oracle.truffle.wasm.constants.Instructions.I64_EQZ;
import static com.oracle.truffle.wasm.constants.Instructions.I64_EXTEND_I32_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_EXTEND_I32_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_GE_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_GE_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_GT_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_GT_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LE_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LE_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LOAD;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LOAD16_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LOAD16_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LOAD32_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LOAD32_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LOAD8_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LOAD8_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LT_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_LT_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_MUL;
import static com.oracle.truffle.wasm.constants.Instructions.I64_NE;
import static com.oracle.truffle.wasm.constants.Instructions.I64_OR;
import static com.oracle.truffle.wasm.constants.Instructions.I64_POPCNT;
import static com.oracle.truffle.wasm.constants.Instructions.I64_REINTERPRET_F64;
import static com.oracle.truffle.wasm.constants.Instructions.I64_REM_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_REM_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_ROTL;
import static com.oracle.truffle.wasm.constants.Instructions.I64_ROTR;
import static com.oracle.truffle.wasm.constants.Instructions.I64_SHL;
import static com.oracle.truffle.wasm.constants.Instructions.I64_SHR_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_SHR_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_STORE;
import static com.oracle.truffle.wasm.constants.Instructions.I64_STORE_16;
import static com.oracle.truffle.wasm.constants.Instructions.I64_STORE_32;
import static com.oracle.truffle.wasm.constants.Instructions.I64_STORE_8;
import static com.oracle.truffle.wasm.constants.Instructions.I64_SUB;
import static com.oracle.truffle.wasm.constants.Instructions.I64_TRUNC_F32_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_TRUNC_F32_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_TRUNC_F64_S;
import static com.oracle.truffle.wasm.constants.Instructions.I64_TRUNC_F64_U;
import static com.oracle.truffle.wasm.constants.Instructions.I64_XOR;
import static com.oracle.truffle.wasm.constants.Instructions.IF;
import static com.oracle.truffle.wasm.constants.Instructions.LOCAL_GET;
import static com.oracle.truffle.wasm.constants.Instructions.LOCAL_SET;
import static com.oracle.truffle.wasm.constants.Instructions.LOCAL_TEE;
import static com.oracle.truffle.wasm.constants.Instructions.LOOP;
import static com.oracle.truffle.wasm.constants.Instructions.MEMORY_GROW;
import static com.oracle.truffle.wasm.constants.Instructions.MEMORY_SIZE;
import static com.oracle.truffle.wasm.constants.Instructions.NOP;
import static com.oracle.truffle.wasm.constants.Instructions.RETURN;
import static com.oracle.truffle.wasm.constants.Instructions.SELECT;
import static com.oracle.truffle.wasm.constants.Instructions.UNREACHABLE;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.wasm.Assert;
import com.oracle.truffle.wasm.BinaryStreamParser;
import com.oracle.truffle.wasm.ValueTypes;
import com.oracle.truffle.wasm.WasmCodeEntry;
import com.oracle.truffle.wasm.WasmContext;
import com.oracle.truffle.wasm.WasmFunction;
import com.oracle.truffle.wasm.WasmLanguage;
import com.oracle.truffle.wasm.WasmModule;
import com.oracle.truffle.wasm.constants.GlobalResolution;
import com.oracle.truffle.wasm.constants.TargetOffset;
import com.oracle.truffle.wasm.exception.WasmExecutionException;
import com.oracle.truffle.wasm.exception.WasmTrap;
import com.oracle.truffle.wasm.memory.WasmMemory;
import com.oracle.truffle.wasm.memory.WasmMemoryException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public final class WasmBlockNode extends WasmNode implements RepeatingNode {

    /**
     * The number of bytes in the byte constant table used by this node.
     */
    @CompilationFinal private int byteConstantLength;

    /**
     * The number of integers in the int constant table used by this node.
     */
    @CompilationFinal private int intConstantLength;

    /**
     * The number of literals in the numeric literals table used by this node.
     */
    @CompilationFinal private int longConstantLength;

    /**
     * The number of branch tables used by this node.
     */
    @CompilationFinal private int branchTableLength;

    @CompilationFinal private final int startOffset;
    @CompilationFinal private final byte returnTypeId;
    @CompilationFinal private final byte continuationTypeId;
    @CompilationFinal private final int initialStackPointer;
    @CompilationFinal private final int initialByteConstantOffset;
    @CompilationFinal private final int initialIntConstantOffset;
    @CompilationFinal private final int initialLongConstantOffset;
    @CompilationFinal private final int initialBranchTableOffset;
    @CompilationFinal private ContextReference<WasmContext> rawContextReference;
    @Children private WasmNode[] nestedControlTable;
    @Children private Node[] callNodeTable;

    public WasmBlockNode(WasmModule wasmModule, WasmCodeEntry codeEntry, int startOffset, byte returnTypeId, byte continuationTypeId, int initialStackPointer,
                    int initialByteConstantOffset, int initialIntConstantOffset, int initialLongConstantOffset, int initialBranchTableOffset) {
        super(wasmModule, codeEntry, -1);
        this.startOffset = startOffset;
        this.returnTypeId = returnTypeId;
        this.continuationTypeId = continuationTypeId;
        this.initialStackPointer = initialStackPointer;
        this.initialByteConstantOffset = initialByteConstantOffset;
        this.initialIntConstantOffset = initialIntConstantOffset;
        this.initialLongConstantOffset = initialLongConstantOffset;
        this.initialBranchTableOffset = initialBranchTableOffset;
        this.nestedControlTable = null;
        this.callNodeTable = null;
    }

    private ContextReference<WasmContext> contextReference() {
        if (rawContextReference == null) {
            rawContextReference = lookupContextReference(WasmLanguage.class);
        }
        return rawContextReference;
    }

    public final void initialize(WasmNode[] nestedControlTable, Node[] callNodeTable, int byteLength, int byteConstantLength,
                    int intConstantLength, int longConstantLength, int branchTableLength) {
        initialize(byteLength);
        this.nestedControlTable = nestedControlTable;
        this.callNodeTable = callNodeTable;
        this.byteConstantLength = byteConstantLength;
        this.intConstantLength = intConstantLength;
        this.longConstantLength = longConstantLength;
        this.branchTableLength = branchTableLength;
    }

    @Override
    int byteConstantLength() {
        return byteConstantLength;
    }

    @Override
    int intConstantLength() {
        return intConstantLength;
    }

    @Override
    int longConstantLength() {
        return longConstantLength;
    }

    @Override
    int branchTableLength() {
        return branchTableLength;
    }

    @Override
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    public TargetOffset execute(WasmContext context, VirtualFrame frame) {
        int nestedControlOffset = 0;
        int callNodeOffset = 0;
        int byteConstantOffset = initialByteConstantOffset;
        int intConstantOffset = initialIntConstantOffset;
        int longConstantOffset = initialLongConstantOffset;
        int branchTableOffset = initialBranchTableOffset;
        int stackPointer = initialStackPointer;
        int offset = startOffset;
        trace("block/if/loop EXECUTE");
        while (offset < startOffset + byteLength()) {
            byte byteOpcode = BinaryStreamParser.peek1(codeEntry().data(), offset);
            int opcode = byteOpcode & 0xFF;
            offset++;
            CompilerAsserts.partialEvaluationConstant(offset);
            switch (opcode) {
                case UNREACHABLE:
                    trace("unreachable");
                    throw new WasmTrap(this, "unreachable");
                case NOP:
                    trace("noop");
                    break;
                case BLOCK: {
                    WasmNode block = nestedControlTable[nestedControlOffset];

                    // The unwind counter indicates how many levels up we need to branch from within
                    // the block.
                    trace("block ENTER");
                    TargetOffset unwindCounter = block.execute(context, frame);
                    trace("block EXIT, target = %d", unwindCounter.value);
                    if (unwindCounter.isGreaterThanZero()) {
                        return unwindCounter.decrement();
                    }

                    nestedControlOffset++;
                    offset += block.byteLength();
                    stackPointer += block.returnTypeLength();
                    byteConstantOffset += block.byteConstantLength();
                    intConstantOffset += block.intConstantLength();
                    longConstantOffset += block.longConstantLength();
                    branchTableOffset += block.branchTableLength();
                    break;
                }
                case LOOP: {
                    WasmNode loopNode = nestedControlTable[nestedControlOffset];

                    // The unwind counter indicates how many levels up we need to branch from within
                    // the loop block.
                    // There are three possibilities for the value of unwind counter:
                    // - A value equal to -1 indicates normal loop completion and that the flow
                    // should continue after the loop end (break out of the loop).
                    // - A value equal to 0 indicates that we need to branch to the beginning of the
                    // loop (repeat loop).
                    // This is handled internally by Truffle and the executing loop should never
                    // return 0 here.
                    // - A value larger than 0 indicates that we need to branch to a level
                    // "shallower" than the current loop block
                    // (break out of the loop and even further).
                    trace("loop ENTER");
                    TargetOffset unwindCounter = loopNode.execute(context, frame);
                    trace("loop EXIT, target = %d", unwindCounter.value);
                    if (unwindCounter.isGreaterThanZero()) {
                        return unwindCounter.decrement();
                    }
                    // The unwind counter cannot be 0 at this point, because that corresponds to
                    // CONTINUE_LOOP_STATUS.
                    assert unwindCounter.isMinusOne() : "Unwind counter after loop exit: " + unwindCounter.value;

                    nestedControlOffset++;
                    offset += loopNode.byteLength();
                    stackPointer += loopNode.returnTypeLength();
                    byteConstantOffset += loopNode.byteConstantLength();
                    intConstantOffset += loopNode.intConstantLength();
                    longConstantOffset += loopNode.longConstantLength();
                    branchTableOffset += loopNode.branchTableLength();
                    break;
                }
                case IF: {
                    WasmNode ifNode = nestedControlTable[nestedControlOffset];
                    stackPointer--;
                    trace("if ENTER");
                    TargetOffset unwindCounter = ifNode.execute(context, frame);
                    trace("if EXIT, target = %d", unwindCounter.value);
                    if (unwindCounter.isGreaterThanZero()) {
                        return unwindCounter.decrement();
                    }
                    nestedControlOffset++;
                    offset += ifNode.byteLength();
                    stackPointer += ifNode.returnTypeLength();
                    byteConstantOffset += ifNode.byteConstantLength();
                    intConstantOffset += ifNode.intConstantLength();
                    longConstantOffset += ifNode.longConstantLength();
                    branchTableOffset += ifNode.branchTableLength();
                    break;
                }
                case ELSE:
                    break;
                case END:
                    break;
                case BR: {
                    int unwindCounterValue = codeEntry().longConstantAsInt(longConstantOffset);
                    TargetOffset unwindCounter = TargetOffset.createOrCached(unwindCounterValue);

                    // Reset the stack pointer to the target block stack pointer.
                    int continuationStackPointer = codeEntry().intConstant(intConstantOffset);
                    int targetBlockReturnLength = codeEntry().intConstant(intConstantOffset + 1);
                    // Technically, we should increment the intConstantOffset and longConstantOffset
                    // at this point,
                    // but since we are returning, it does not really matter.

                    trace("br, target = %d", unwindCounterValue);

                    // Populate the stack with the return values of the current block (the one we
                    // are escaping from).
                    unwindStack(frame, stackPointer, continuationStackPointer, targetBlockReturnLength);

                    return unwindCounter;
                }
                case BR_IF: {
                    stackPointer--;
                    if (popCondition(frame, stackPointer)) {
                        int unwindCounterValue = codeEntry().longConstantAsInt(longConstantOffset);
                        TargetOffset unwindCounter = TargetOffset.createOrCached(unwindCounterValue);

                        // Reset the stack pointer to the target block stack pointer.
                        int continuationStackPointer = codeEntry().intConstant(intConstantOffset);
                        int targetBlockReturnLength = codeEntry().intConstant(intConstantOffset + 1);
                        // Technically, we should increment the intConstantOffset and
                        // longConstantOffset at this point,
                        // but since we are returning, it does not really matter.

                        trace("br_if, target = %d", unwindCounterValue);

                        // Populate the stack with the return values of the current block (the one
                        // we are escaping from).
                        unwindStack(frame, stackPointer, continuationStackPointer, targetBlockReturnLength);

                        return unwindCounter;
                    }
                    longConstantOffset++;
                    intConstantOffset += 2;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;
                    break;
                }
                case BR_TABLE: {
                    stackPointer--;
                    int index = popInt(frame, stackPointer);
                    int[] table = codeEntry().branchTable(branchTableOffset);
                    index = index < 0 || index >= (table.length - 1) / 2 ? (table.length - 1) / 2 - 1 : index;
                    // Technically, we should increment the branchTableOffset at this point,
                    // but since we are returning, it does not really matter.

                    int returnTypeLength = table[0];
                    int targetOffsetValue = table[1 + 2 * index];
                    int continuationStackPointer = table[1 + 2 * index + 1];
                    trace("br_table, target = %d", targetOffsetValue);

                    // Populate the stack with the return values of the current block (the one we
                    // are escaping from).
                    unwindStack(frame, stackPointer, continuationStackPointer, returnTypeLength);

                    return TargetOffset.createOrCached(targetOffsetValue);
                }
                case RETURN: {
                    // A return statement causes the termination of the current function, i.e.
                    // causes the execution
                    // to resume after the instruction that invoked the current frame.
                    int unwindCounterValue = codeEntry().longConstantAsInt(longConstantOffset);
                    int rootBlockReturnLength = codeEntry().intConstant(intConstantOffset);
                    unwindStack(frame, stackPointer, 0, rootBlockReturnLength);
                    trace("return");
                    return TargetOffset.createOrCached(unwindCounterValue);
                }
                case CALL: {
                    int functionIndex = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;

                    WasmFunction function = module().symbolTable().function(functionIndex);
                    byte returnType = function.returnType();
                    int numArgs = function.numArguments();

                    if (callNodeTable[callNodeOffset] instanceof WasmCallStubNode) {
                        // Lazily create the direct call node at this code position, and recompile
                        // to eliminate this check.
                        resolveCallNode(callNodeOffset);
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                    }
                    DirectCallNode callNode = (DirectCallNode) callNodeTable[callNodeOffset];
                    callNodeOffset++;

                    Object[] args = createArgumentsForCall(frame, function, numArgs, stackPointer);
                    stackPointer -= args.length;

                    trace("direct call to function %s (%d args)", function, args.length);
                    Object result = callNode.call(args);
                    trace("return from direct call to function %s : %s", function, result);
                    // At the moment, WebAssembly functions may return up to one value.
                    // As per the WebAssembly specification,
                    // this restriction may be lifted in the future.
                    switch (returnType) {
                        case ValueTypes.I32_TYPE: {
                            pushInt(frame, stackPointer, (int) result);
                            stackPointer++;
                            break;
                        }
                        case ValueTypes.I64_TYPE: {
                            push(frame, stackPointer, (long) result);
                            stackPointer++;
                            break;
                        }
                        case ValueTypes.F32_TYPE: {
                            pushFloat(frame, stackPointer, (float) result);
                            stackPointer++;
                            break;
                        }
                        case ValueTypes.F64_TYPE: {
                            pushDouble(frame, stackPointer, (double) result);
                            stackPointer++;
                            break;
                        }
                        default: {
                            // Void return type - do nothing.
                            break;
                        }
                    }

                    break;
                }
                case CALL_INDIRECT: {
                    // Extract the function object.
                    stackPointer--;
                    final int tableIndex = module().symbolTable().tableIndex();
                    final Object[] table = context.tables().table(tableIndex);
                    final int elementIndex = popInt(frame, stackPointer);
                    if (elementIndex < 0 || elementIndex >= table.length) {
                        throw new WasmTrap(this, "Element index out of table bounds.");
                    }
                    // Currently, table elements may only be functions.
                    // We can add a check here when this changes in the future.
                    final WasmFunction function = (WasmFunction) table[elementIndex];

                    // Extract the function type index.
                    int expectedFunctionTypeIndex = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;
                    // Consume the ZERO_TABLE constant at the end of the CALL_INDIRECT instruction.
                    offset += 1;

                    // Validate that the function type matches the expected type.
                    if (expectedFunctionTypeIndex != function.typeIndex()) {
                        // TODO: This check may be too rigorous, as the WebAssembly specification
                        // seems to allow
                        // multiple definitions of the same type. We should refine the check.
                        // Alternatively, we should maybe refine our modules to avoid function type
                        // redefinition --
                        // the predefined modules may currently redefine a type.
                        throw new WasmTrap(this, format("Actual (%d) and expected (%d) function types differ in the indirect call.",
                                        function.typeIndex(), expectedFunctionTypeIndex));
                    }

                    // Invoke the resolved function.
                    IndirectCallNode callNode = (IndirectCallNode) callNodeTable[callNodeOffset];
                    callNodeOffset++;

                    int numArgs = module().symbolTable().functionTypeArgumentCount(expectedFunctionTypeIndex);
                    Object[] args = createArgumentsForCall(frame, function, numArgs, stackPointer);
                    stackPointer -= args.length;

                    trace("indirect call to function %s (%d args)", function, args.length);
                    Object result = callNode.call(function.resolveCallTarget(), args);
                    trace("return from indirect_call to function %s : %s", function, result);
                    // At the moment, WebAssembly functions may return up to one value.
                    // As per the WebAssembly specification, this restriction may be lifted in the
                    // future.
                    switch (function.returnType()) {
                        case ValueTypes.I32_TYPE: {
                            pushInt(frame, stackPointer, (int) result);
                            stackPointer++;
                            break;
                        }
                        case ValueTypes.I64_TYPE: {
                            push(frame, stackPointer, (long) result);
                            stackPointer++;
                            break;
                        }
                        case ValueTypes.F32_TYPE: {
                            pushFloat(frame, stackPointer, (float) result);
                            stackPointer++;
                            break;
                        }
                        case ValueTypes.F64_TYPE: {
                            pushDouble(frame, stackPointer, (double) result);
                            stackPointer++;
                            break;
                        }
                        default:
                            // Void return type - do nothing.
                            break;
                    }

                    break;
                }
                case DROP: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    trace("drop (raw long value = 0x%016X)", x);
                    break;
                }
                case SELECT: {
                    stackPointer--;
                    int cond = popInt(frame, stackPointer);
                    stackPointer--;
                    long val2 = pop(frame, stackPointer);
                    stackPointer--;
                    long val1 = pop(frame, stackPointer);
                    push(frame, stackPointer, cond != 0 ? val1 : val2);
                    stackPointer++;
                    trace("select 0x%08X ? 0x%08X : 0x%08X = 0x%08X", cond, val1, val2, cond != 0 ? val1 : val2);
                    break;
                }
                case LOCAL_GET: {
                    int index = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;
                    byte type = codeEntry().localType(index);
                    switch (type) {
                        case ValueTypes.I32_TYPE: {
                            int value = getInt(frame, index);
                            pushInt(frame, stackPointer, value);
                            stackPointer++;
                            trace("local.get %d, value = 0x%08X (%d) [i32]", index, value, value);
                            break;
                        }
                        case ValueTypes.I64_TYPE: {
                            long value = getLong(frame, index);
                            push(frame, stackPointer, value);
                            stackPointer++;
                            trace("local.get %d, value = 0x%016X (%d) [i64]", index, value, value);
                            break;
                        }
                        case ValueTypes.F32_TYPE: {
                            float value = getFloat(frame, index);
                            pushFloat(frame, stackPointer, value);
                            stackPointer++;
                            trace("local.get %d, value = %f [f32]", index, value);
                            break;
                        }
                        case ValueTypes.F64_TYPE: {
                            double value = getDouble(frame, index);
                            pushDouble(frame, stackPointer, value);
                            stackPointer++;
                            trace("local.get %d, value = %f [f64]", index, value);
                            break;
                        }
                    }
                    break;
                }
                case LOCAL_SET: {
                    int index = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;
                    byte type = codeEntry().localType(index);
                    switch (type) {
                        case ValueTypes.I32_TYPE: {
                            stackPointer--;
                            int value = popInt(frame, stackPointer);
                            setInt(frame, index, value);
                            trace("local.set %d, value = 0x%08X (%d) [i32]", index, value, value);
                            break;
                        }
                        case ValueTypes.I64_TYPE: {
                            stackPointer--;
                            long value = pop(frame, stackPointer);
                            setLong(frame, index, value);
                            trace("local.set %d, value = 0x%016X (%d) [i64]", index, value, value);
                            break;
                        }
                        case ValueTypes.F32_TYPE: {
                            stackPointer--;
                            float value = popAsFloat(frame, stackPointer);
                            setFloat(frame, index, value);
                            trace("local.set %d, value = %f [f32]", index, value);
                            break;
                        }
                        case ValueTypes.F64_TYPE: {
                            stackPointer--;
                            double value = popAsDouble(frame, stackPointer);
                            setDouble(frame, index, value);
                            trace("local.set %d, value = %f [f64]", index, value);
                            break;
                        }
                    }
                    break;
                }
                case LOCAL_TEE: {
                    int index = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;
                    byte type = codeEntry().localType(index);
                    switch (type) {
                        case ValueTypes.I32_TYPE: {
                            stackPointer--;
                            int value = popInt(frame, stackPointer);
                            pushInt(frame, stackPointer, value);
                            stackPointer++;
                            setInt(frame, index, value);
                            trace("local.tee %d, value = 0x%08X (%d) [i32]", index, value, value);
                            break;
                        }
                        case ValueTypes.I64_TYPE: {
                            stackPointer--;
                            long value = pop(frame, stackPointer);
                            push(frame, stackPointer, value);
                            stackPointer++;
                            setLong(frame, index, value);
                            trace("local.tee %d, value = 0x%016X (%d) [i64]", index, value, value);
                            break;
                        }
                        case ValueTypes.F32_TYPE: {
                            stackPointer--;
                            float value = popAsFloat(frame, stackPointer);
                            pushFloat(frame, stackPointer, value);
                            stackPointer++;
                            setFloat(frame, index, value);
                            trace("local.tee %d, value = %f [f32]", index, value);
                            break;
                        }
                        case ValueTypes.F64_TYPE: {
                            stackPointer--;
                            double value = popAsDouble(frame, stackPointer);
                            pushDouble(frame, stackPointer, value);
                            stackPointer++;
                            setDouble(frame, index, value);
                            trace("local.tee %d, value = %f [f64]", index, value);
                            break;
                        }
                    }
                    break;
                }
                case GLOBAL_GET: {
                    int index = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;

                    final GlobalResolution resolution = module().symbolTable().globalResolution(index);
                    if (!resolution.isResolved()) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw new WasmExecutionException(this, "Globals should be resolved before runtime.");
                    }

                    byte type = module().symbolTable().globalValueType(index);
                    switch (type) {
                        case ValueTypes.I32_TYPE: {
                            int address = module().symbolTable().globalAddress(index);
                            int value = context.globals().loadAsInt(address);
                            pushInt(frame, stackPointer, value);
                            stackPointer++;
                            trace("global.get %d, value = 0x%08X (%d) [i32]", index, value, value);
                            break;
                        }
                        case ValueTypes.I64_TYPE: {
                            int address = module().symbolTable().globalAddress(index);
                            long value = context.globals().loadAsLong(address);
                            push(frame, stackPointer, value);
                            stackPointer++;
                            trace("global.get %d, value = 0x%016X (%d) [i64]", index, value, value);
                            break;
                        }
                        case ValueTypes.F32_TYPE: {
                            int address = module().symbolTable().globalAddress(index);
                            int value = context.globals().loadAsInt(address);
                            pushInt(frame, stackPointer, value);
                            stackPointer++;
                            trace("global.get %d, value = %f [f32]", index, Float.intBitsToFloat(value));
                            break;
                        }
                        case ValueTypes.F64_TYPE: {
                            int address = module().symbolTable().globalAddress(index);
                            long value = context.globals().loadAsLong(address);
                            push(frame, stackPointer, value);
                            stackPointer++;
                            trace("global.get %d, value = %f [f64]", index, Double.longBitsToDouble(value));
                            break;
                        }
                    }
                    break;
                }
                case GLOBAL_SET: {
                    int index = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;

                    final GlobalResolution resolution = module().symbolTable().globalResolution(index);
                    if (!resolution.isResolved()) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw new WasmExecutionException(this, "Globals should be resolved before runtime.");
                    }

                    byte type = module().symbolTable().globalValueType(index);
                    // For global.set, we don't need to make sure that the referenced global is
                    // mutable.
                    // This is taken care of by validation during wat to wasm compilation.
                    switch (type) {
                        case ValueTypes.I32_TYPE: {
                            stackPointer--;
                            int value = popInt(frame, stackPointer);
                            int address = module().symbolTable().globalAddress(index);
                            context.globals().storeInt(address, value);
                            trace("global.set %d, value = 0x%08X (%d) [i32]", index, value, value);
                            break;
                        }
                        case ValueTypes.I64_TYPE: {
                            stackPointer--;
                            long value = pop(frame, stackPointer);
                            int address = module().symbolTable().globalAddress(index);
                            context.globals().storeLong(address, value);
                            trace("global.set %d, value = 0x%016X (%d) [i64]", index, value, value);
                            break;
                        }
                        case ValueTypes.F32_TYPE: {
                            stackPointer--;
                            int value = popInt(frame, stackPointer);
                            int address = module().symbolTable().globalAddress(index);
                            context.globals().storeFloatWithInt(address, value);
                            trace("global.set %d, value = %f [f32]", index, Float.intBitsToFloat(value));
                            break;
                        }
                        case ValueTypes.F64_TYPE: {
                            stackPointer--;
                            long value = pop(frame, stackPointer);
                            int address = module().symbolTable().globalAddress(index);
                            context.globals().storeDoubleWithLong(address, value);
                            trace("global.set %d, value = %f [f64]", index, Double.longBitsToDouble(value));
                            break;
                        }
                    }
                    break;
                }
                case I32_LOAD:
                case I64_LOAD:
                case F32_LOAD:
                case F64_LOAD:
                case I32_LOAD8_S:
                case I32_LOAD8_U:
                case I32_LOAD16_S:
                case I32_LOAD16_U:
                case I64_LOAD8_S:
                case I64_LOAD8_U:
                case I64_LOAD16_S:
                case I64_LOAD16_U:
                case I64_LOAD32_S:
                case I64_LOAD32_U: {
                    /* The memAlign hint is not currently used or taken into account. */
                    byte memAlignConstantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += memAlignConstantLength;

                    int memOffset = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte memOffsetConstantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += memOffsetConstantLength;

                    stackPointer--;
                    int baseAddress = popInt(frame, stackPointer);
                    int address = baseAddress + memOffset;
                    WasmMemory memory = module().symbolTable().memory();

                    try {
                        switch (opcode) {
                            case I32_LOAD: {
                                memory.validateAddress(address, 32);
                                int value = memory.load_i32(address);
                                pushInt(frame, stackPointer, value);
                                break;
                            }
                            case I64_LOAD: {
                                memory.validateAddress(address, 64);
                                long value = memory.load_i64(address);
                                push(frame, stackPointer, value);
                                break;
                            }
                            case F32_LOAD: {
                                memory.validateAddress(address, 32);
                                float value = memory.load_f32(address);
                                pushFloat(frame, stackPointer, value);
                                break;
                            }
                            case F64_LOAD: {
                                memory.validateAddress(address, 64);
                                double value = memory.load_f64(address);
                                pushDouble(frame, stackPointer, value);
                                break;
                            }
                            case I32_LOAD8_S: {
                                memory.validateAddress(address, 8);
                                int value = memory.load_i32_8s(address);
                                pushInt(frame, stackPointer, value);
                                break;
                            }
                            case I32_LOAD8_U: {
                                memory.validateAddress(address, 8);
                                int value = memory.load_i32_8u(address);
                                pushInt(frame, stackPointer, value);
                                break;
                            }
                            case I32_LOAD16_S: {
                                memory.validateAddress(address, 16);
                                int value = memory.load_i32_16s(address);
                                pushInt(frame, stackPointer, value);
                                break;
                            }
                            case I32_LOAD16_U: {
                                memory.validateAddress(address, 16);
                                int value = memory.load_i32_16u(address);
                                pushInt(frame, stackPointer, value);
                                break;
                            }
                            case I64_LOAD8_S: {
                                memory.validateAddress(address, 8);
                                long value = memory.load_i64_8s(address);
                                push(frame, stackPointer, value);
                                break;
                            }
                            case I64_LOAD8_U: {
                                memory.validateAddress(address, 8);
                                long value = memory.load_i64_8u(address);
                                push(frame, stackPointer, value);
                                break;
                            }
                            case I64_LOAD16_S: {
                                memory.validateAddress(address, 16);
                                long value = memory.load_i64_16s(address);
                                push(frame, stackPointer, value);
                                break;
                            }
                            case I64_LOAD16_U: {
                                memory.validateAddress(address, 16);
                                long value = memory.load_i64_16u(address);
                                push(frame, stackPointer, value);
                                break;
                            }
                            case I64_LOAD32_S: {
                                memory.validateAddress(address, 32);
                                long value = memory.load_i64_32s(address);
                                push(frame, stackPointer, value);
                                break;
                            }
                            case I64_LOAD32_U: {
                                memory.validateAddress(address, 32);
                                long value = memory.load_i64_32u(address);
                                push(frame, stackPointer, value);
                                break;
                            }
                        }
                    } catch (WasmMemoryException e) {
                        throw new WasmTrap(this, "memory address out-of-bounds");
                    }
                    stackPointer++;
                    break;
                }
                case I32_STORE:
                case I64_STORE:
                case F32_STORE:
                case F64_STORE:
                case I32_STORE_8:
                case I32_STORE_16:
                case I64_STORE_8:
                case I64_STORE_16:
                case I64_STORE_32: {
                    /* The memAlign hint is not currently used or taken into account. */
                    byte memAlignConstantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += memAlignConstantLength;

                    int memOffset = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte memOffsetConstantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += memOffsetConstantLength;
                    WasmMemory memory = module().symbolTable().memory();

                    try {
                        switch (opcode) {
                            case I32_STORE: {
                                stackPointer--;
                                int value = popInt(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 32);
                                memory.store_i32(address, value);
                                break;
                            }
                            case I64_STORE: {
                                stackPointer--;
                                long value = pop(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 64);
                                memory.store_i64(address, value);
                                break;
                            }
                            case F32_STORE: {
                                stackPointer--;
                                float value = popAsFloat(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 32);
                                memory.store_f32(address, value);
                                break;
                            }
                            case F64_STORE: {
                                stackPointer--;
                                double value = popAsDouble(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 64);
                                memory.store_f64(address, value);
                                break;
                            }
                            case I32_STORE_8: {
                                stackPointer--;
                                int value = popInt(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 8);
                                memory.store_i32_8(address, (byte) value);
                                break;
                            }
                            case I32_STORE_16: {
                                stackPointer--;
                                int value = popInt(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 16);
                                memory.store_i32_16(address, (short) value);
                                break;
                            }
                            case I64_STORE_8: {
                                stackPointer--;
                                long value = pop(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 8);
                                memory.store_i64_8(address, (byte) value);
                                break;
                            }
                            case I64_STORE_16: {
                                stackPointer--;
                                long value = pop(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 16);
                                memory.store_i64_16(address, (short) value);
                                break;
                            }
                            case I64_STORE_32: {
                                stackPointer--;
                                long value = pop(frame, stackPointer);
                                stackPointer--;
                                int baseAddress = popInt(frame, stackPointer);
                                int address = baseAddress + memOffset;
                                memory.validateAddress(address, 32);
                                memory.store_i64_32(address, (int) value);
                                break;
                            }
                        }
                    } catch (WasmMemoryException e) {
                        throw new WasmTrap(this, "memory address out-of-bounds");
                    }

                    break;
                }
                case MEMORY_SIZE: {
                    // Skip the 0x00 constant.
                    offset++;
                    trace("memory_size");
                    int pageSize = (int) (module().symbolTable().memory().pageSize());
                    pushInt(frame, stackPointer, pageSize);
                    stackPointer++;
                    break;
                }
                case MEMORY_GROW: {
                    // Skip the 0x00 constant.
                    offset++;
                    trace("memory_grow");
                    stackPointer--;
                    int extraSize = popInt(frame, stackPointer);
                    final WasmMemory memory = module().symbolTable().memory();
                    int pageSize = (int) memory.pageSize();
                    if (memory.grow(extraSize)) {
                        pushInt(frame, stackPointer, pageSize);
                        stackPointer++;
                    } else {
                        pushInt(frame, stackPointer, -1);
                        stackPointer++;
                    }
                    break;
                }
                case I32_CONST: {
                    int value = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;
                    pushInt(frame, stackPointer, value);
                    stackPointer++;
                    trace("i32.const 0x%08X (%d)", value, value);
                    break;
                }
                case I64_CONST: {
                    long value = codeEntry().longConstant(longConstantOffset);
                    longConstantOffset++;
                    byte constantLength = codeEntry().byteConstant(byteConstantOffset);
                    byteConstantOffset++;
                    offset += constantLength;
                    push(frame, stackPointer, value);
                    stackPointer++;
                    trace("i64.const 0x%016X (%d)", value, value);
                    break;
                }
                case I32_EQZ: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, x == 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X == 0x%08X ? [i32]", x, 0);
                    break;
                }
                case I32_EQ: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, y == x ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X == 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_NE: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, y != x ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X != 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_LT_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, y < x ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X < 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_LT_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, Integer.compareUnsigned(y, x) < 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X <u 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_GT_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, y > x ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X > 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_GT_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, Integer.compareUnsigned(y, x) > 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X >u 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_LE_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, y <= x ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X <= 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_LE_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, Integer.compareUnsigned(y, x) <= 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X <=u 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_GE_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, y >= x ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X >= 0x%08X ? [i32]", y, x);
                    break;
                }
                case I32_GE_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, Integer.compareUnsigned(y, x) >= 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%08X >=u 0x%08X ? [i32]", y, x);
                    break;
                }
                case I64_EQZ: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, x == 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X == 0x%016X ? [i64]", x, 0);
                    break;
                }
                case I64_EQ: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, y == x ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X == 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_NE: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, y != x ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X != 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_LT_S: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, y < x ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X < 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_LT_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, Long.compareUnsigned(y, x) < 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X <u 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_GT_S: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, y > x ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X > 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_GT_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, Long.compareUnsigned(y, x) > 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X >u 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_LE_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    pushInt(frame, stackPointer, y <= x ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X <= 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_LE_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, Long.compareUnsigned(y, x) <= 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X <=u 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_GE_S: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, y >= x ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X >= 0x%016X ? [i64]", y, x);
                    break;
                }
                case I64_GE_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    pushInt(frame, stackPointer, Long.compareUnsigned(y, x) >= 0 ? 1 : 0);
                    stackPointer++;
                    trace("0x%016X >=u 0x%016X ? [i64]", y, x);
                    break;
                }
                case F32_EQ: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    pushInt(frame, stackPointer, y == x ? 1 : 0);
                    stackPointer++;
                    trace("%f == %f ? [f32]", y, x);
                    break;
                }
                case F32_NE: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    pushInt(frame, stackPointer, y != x ? 1 : 0);
                    stackPointer++;
                    trace("%f != %f ? [f32]", y, x);
                    break;
                }
                case F32_LT: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    pushInt(frame, stackPointer, y < x ? 1 : 0);
                    stackPointer++;
                    trace("%f < %f ? [f32]", y, x);
                    break;
                }
                case F32_GT: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    pushInt(frame, stackPointer, y > x ? 1 : 0);
                    stackPointer++;
                    trace("%f > %f ? [f32]", y, x);
                    break;
                }
                case F32_LE: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    pushInt(frame, stackPointer, y <= x ? 1 : 0);
                    stackPointer++;
                    trace("%f <= %f ? [f32]", y, x);
                    break;
                }
                case F32_GE: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    pushInt(frame, stackPointer, y >= x ? 1 : 0);
                    stackPointer++;
                    trace("%f >= %f ? [f32]", y, x);
                    break;
                }
                case F64_EQ: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    pushInt(frame, stackPointer, y == x ? 1 : 0);
                    stackPointer++;
                    trace("%f == %f ? [f64]", y, x);
                    break;
                }
                case F64_NE: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    pushInt(frame, stackPointer, y != x ? 1 : 0);
                    stackPointer++;
                    trace("%f != %f ? [f64]", y, x);
                    break;
                }
                case F64_LT: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    pushInt(frame, stackPointer, y < x ? 1 : 0);
                    stackPointer++;
                    trace("%f < %f ? [f64]", y, x);
                    break;
                }
                case F64_GT: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    pushInt(frame, stackPointer, y > x ? 1 : 0);
                    stackPointer++;
                    trace("%f > %f ? [f64]", y, x);
                    break;
                }
                case F64_LE: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    pushInt(frame, stackPointer, y <= x ? 1 : 0);
                    stackPointer++;
                    trace("%f <= %f ? [f64]", y, x);
                    break;
                }
                case F64_GE: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    pushInt(frame, stackPointer, y >= x ? 1 : 0);
                    stackPointer++;
                    trace("%f >= %f ? [f64]", y, x);
                    break;
                }
                case I32_CLZ: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    int result = Integer.numberOfLeadingZeros(x);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("clz(0x%08X) = %d [i32]", x, result);
                    break;
                }
                case I32_CTZ: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    int result = Integer.numberOfTrailingZeros(x);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("ctz(0x%08X) = %d [i32]", x, result);
                    break;
                }
                case I32_POPCNT: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    int result = Integer.bitCount(x);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("popcnt(0x%08X) = %d [i32]", x, result);
                    break;
                }
                case I32_ADD: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y + x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X + 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_SUB: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y - x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X - 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_MUL: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y * x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X * 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_DIV_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y / x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X / 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_DIV_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = Integer.divideUnsigned(y, x);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X /u 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_REM_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y % x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X %% 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_REM_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = Integer.remainderUnsigned(y, x);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X %%u 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_AND: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y & x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X & 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_OR: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y | x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X | 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_XOR: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y ^ x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X ^ 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_SHL: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y << x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X << 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_SHR_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y >> x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X >> 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_SHR_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = y >>> x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X >>> 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_ROTL: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = Integer.rotateLeft(y, x);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X rotl 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I32_ROTR: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    stackPointer--;
                    int y = popInt(frame, stackPointer);
                    int result = Integer.rotateRight(y, x);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%08X rotr 0x%08X = 0x%08X (%d) [i32]", y, x, result, result);
                    break;
                }
                case I64_CLZ: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    long result = Long.numberOfLeadingZeros(x);
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("clz(0x%016X) = 0x%08X (%d) [i32]", x, result, result);
                    break;
                }
                case I64_CTZ: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    long result = Long.numberOfTrailingZeros(x);
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("ctz(0x%016X) = 0x%08X (%d) [i32]", x, result, result);
                    break;
                }
                case I64_POPCNT: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    long result = Long.bitCount(x);
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("popcnt(0x%016X) = 0x%08X (%d) [i32]", x, result, result);
                    break;
                }
                case I64_ADD: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y + x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X + 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_SUB: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y - x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X - 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_MUL: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y * x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X * 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_DIV_S: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y / x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X / 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_DIV_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = Long.divideUnsigned(y, x);
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X /u 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_REM_S: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y % x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X %% 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_REM_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = Long.remainderUnsigned(y, x);
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X %%u 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_AND: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y & x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X & 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_OR: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y | x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X | 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_XOR: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y ^ x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X ^ 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_SHL: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y << x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X << 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_SHR_S: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y >> x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X >> 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_SHR_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = y >>> x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X >>> 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_ROTL: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = Long.rotateLeft(y, (int) x);
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X rotl 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case I64_ROTR: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    stackPointer--;
                    long y = pop(frame, stackPointer);
                    long result = Long.rotateRight(y, (int) x);
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push 0x%016X rotr 0x%016X = 0x%016X (%d) [i64]", y, x, result, result);
                    break;
                }
                case F32_CONST: {
                    int value = codeEntry().longConstantAsInt(longConstantOffset);
                    longConstantOffset++;
                    offset += 4;
                    pushInt(frame, stackPointer, value);
                    stackPointer++;
                    trace("f32.const %f", Float.intBitsToFloat(value));
                    break;
                }
                case F32_ABS: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    float result = Math.abs(x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("f32.abs(%f) = %f", x, result);
                    break;
                }
                case F32_NEG: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    float result = -x;
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("f32.neg(%f) = %f", x, result);
                    break;
                }
                case F32_CEIL: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    float result = (float) Math.ceil(x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("f32.ceil(%f) = %f", x, result);
                    break;
                }
                case F32_FLOOR: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    float result = (float) Math.floor(x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("f32.floor(%f) = %f", x, result);
                    break;
                }
                case F32_TRUNC: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    float result = (int) x;
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("f32.trunc(%f) = %f", x, result);
                    break;
                }
                case F32_NEAREST: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    float result = Math.round(x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("f32.nearest(%f) = %f", x, result);
                    break;
                }
                case F32_SQRT: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    float result = (float) Math.sqrt(x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("f32.sqrt(%f) = %f", x, result);
                    break;
                }
                case F32_ADD: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    float result = y + x;
                    pushFloat(frame, stackPointer, result);
                    trace("push %f + %f = %f [f32]", y, x, result);
                    stackPointer++;
                    break;
                }
                case F32_SUB: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    float result = y - x;
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push %f - %f = %f [f32]", y, x, result);
                    break;
                }
                case F32_MUL: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    float result = y * x;
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push %f * %f = %f [f32]", y, x, result);
                    break;
                }
                case F32_DIV: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    float result = y / x;
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push %f / %f = %f [f32]", y, x, result);
                    break;
                }
                case F32_MIN: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    float result = Math.min(y, x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push min(%f, %f) = %f [f32]", y, x, result);
                    break;
                }
                case F32_MAX: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    float result = Math.max(y, x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push max(%f, %f) = %f [f32]", y, x, result);
                    break;
                }
                case F32_COPYSIGN: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    stackPointer--;
                    float y = popAsFloat(frame, stackPointer);
                    float result = Math.copySign(y, x);
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push copysign(%f, %f) = %f [f32]", y, x, result);
                    break;
                }
                case F64_CONST: {
                    long value = codeEntry().longConstant(longConstantOffset);
                    longConstantOffset++;
                    offset += 8;
                    push(frame, stackPointer, value);
                    stackPointer++;
                    trace("f64.const %f", Double.longBitsToDouble(value));
                    break;
                }
                case F64_ABS: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    double result = Math.abs(x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("f64.abs(%f) = %f", x, result);
                    break;
                }
                case F64_NEG: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    double result = -x;
                    pushDouble(frame, stackPointer, -x);
                    stackPointer++;
                    trace("f64.neg(%f) = %f", x, result);
                    break;
                }
                case F64_CEIL: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    double result = Math.ceil(x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("f64.ceil(%f) = %f", x, result);
                    break;
                }
                case F64_FLOOR: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    double result = Math.floor(x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("f64.floor(%f) = %f", x, result);
                    break;
                }
                case F64_TRUNC: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    double result = (long) x;
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("f64.trunc(%f) = %f", x, result);
                    break;
                }
                case F64_NEAREST: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    double result = Math.round(x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("f64.nearest(%f) = %f", x, result);
                    break;
                }
                case F64_SQRT: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    double result = Math.sqrt(x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("f64.sqrt(%f) = %f", x, result);
                    break;
                }
                case F64_ADD: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    double result = y + x;
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push %f + %f = %f [f64]", y, x, result);
                    break;
                }
                case F64_SUB: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    double result = y - x;
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push %f - %f = %f [f64]", y, x, result);
                    break;
                }
                case F64_MUL: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    double result = y * x;
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push %f * %f = %f [f64]", y, x, result);
                    break;
                }
                case F64_DIV: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    double result = y / x;
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push %f / %f = %f [f64]", y, x, result);
                    break;
                }
                case F64_MIN: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    double result = Math.min(y, x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push min(%f, %f) = %f [f64]", y, x, result);
                    break;
                }
                case F64_MAX: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    double result = Math.max(y, x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push max(%f, %f) = %f [f64]", y, x, result);
                    break;
                }
                case F64_COPYSIGN: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    stackPointer--;
                    double y = popAsDouble(frame, stackPointer);
                    double result = Math.copySign(y, x);
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push copysign(%f, %f) = %f [f64]", y, x, result);
                    break;
                }
                case I32_WRAP_I64: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    int result = (int) (x % 0x8000_0000);
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push wrap_i64(0x%016X) = 0x%08X (%d) [i32]", x, result, result);
                    break;
                }
                case I32_TRUNC_F32_S:
                case I32_TRUNC_F32_U: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    int result = (int) x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push trunc_f32(%f) = 0x%08X (%d) [i32]", x, result, result);
                    break;
                }
                case I32_TRUNC_F64_S:
                case I32_TRUNC_F64_U: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    int result = (int) x;
                    pushInt(frame, stackPointer, result);
                    stackPointer++;
                    trace("push trunc_f64(%f) = 0x%08X (%d) [i32]", x, result, result);
                    break;
                }
                case I64_EXTEND_I32_S: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    long result = x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push extend_i32_s(0x%08X) = 0x%016X (%d) [i64]", x, result, result);
                    break;
                }
                case I64_EXTEND_I32_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    long result = x & 0xFFFF_FFFFL;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push extend_i32_u(0x%08X) = 0x%016X (%d) [i64]", x, result, result);
                    break;
                }
                case I64_TRUNC_F32_S:
                case I64_TRUNC_F32_U: {
                    stackPointer--;
                    float x = popAsFloat(frame, stackPointer);
                    long result = (long) x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push trunc_f32(%f) = 0x%016X (%d) [i64]", x, result, result);
                    break;
                }
                case I64_TRUNC_F64_S:
                case I64_TRUNC_F64_U: {
                    stackPointer--;
                    double x = popAsDouble(frame, stackPointer);
                    long result = (long) x;
                    push(frame, stackPointer, result);
                    stackPointer++;
                    trace("push trunc_f64(%f) = 0x%016X (%d) [i64]", x, result, result);
                    break;
                }
                case F32_CONVERT_I32_S:
                case F32_CONVERT_I32_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    float result = (float) x;
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push convert_i32(0x%08X) = %f [f32]", x, result);
                    break;
                }
                case F32_CONVERT_I64_S:
                case F32_CONVERT_I64_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    float result = (float) x;
                    pushFloat(frame, stackPointer, result);
                    stackPointer++;
                    trace("push convert_i64(0x%016X) = %f [f32]", x, result);
                    break;
                }
                case F32_DEMOTE_F64: {
                    throw new NotImplementedException();
                }
                case F64_CONVERT_I32_S:
                case F64_CONVERT_I32_U: {
                    stackPointer--;
                    int x = popInt(frame, stackPointer);
                    double result = (double) x;
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push convert_i32(0x%08X) = %f [f64]", x, result);
                    break;
                }
                case F64_CONVERT_I64_S:
                case F64_CONVERT_I64_U: {
                    stackPointer--;
                    long x = pop(frame, stackPointer);
                    double result = (double) x;
                    pushDouble(frame, stackPointer, result);
                    stackPointer++;
                    trace("push convert_i64(0x%016X) = %f [f64]", x, result);
                    break;
                }
                case F64_PROMOTE_F32: {
                    throw new NotImplementedException();
                }
                case I32_REINTERPRET_F32: {
                    // As we don't store type information for the frame slots (everything is stored
                    // as raw bits in a long,
                    // and interpreted appropriately upon access), we don't need to do anything for
                    // these instructions.
                    trace("push reinterpret_f32 [i32]");
                    break;
                }
                case I64_REINTERPRET_F64: {
                    // As we don't store type information for the frame slots (everything is stored
                    // as raw bits in a long,
                    // and interpreted appropriately upon access), we don't need to do anything for
                    // these instructions.
                    trace("push reinterpret_f64 [i64]");
                    break;
                }
                case F32_REINTERPRET_I32: {
                    // As we don't store type information for the frame slots (everything is stored
                    // as raw bits in a long,
                    // and interpreted appropriately upon access), we don't need to do anything for
                    // these instructions.
                    trace("push reinterpret_i32 [f32]");
                    break;
                }
                case F64_REINTERPRET_I64: {
                    // As we don't store type information for the frame slots (everything is stored
                    // as raw bits in a long,
                    // and interpreted appropriately upon access), we don't need to do anything for
                    // these instructions.
                    trace("push reinterpret_i64 [f64]");
                    break;
                }
                default:
                    Assert.fail(format("Unknown opcode: 0x%02X", opcode));
            }
        }
        return TargetOffset.MINUS_ONE;
    }

    private boolean popCondition(VirtualFrame frame, int stackPointer) {
        int condition = popInt(frame, stackPointer);
        return condition != 0;
    }

    @TruffleBoundary
    private void resolveCallNode(int callNodeOffset) {
        final RootCallTarget target = ((WasmCallStubNode) callNodeTable[callNodeOffset]).function().resolveCallTarget();
        callNodeTable[callNodeOffset] = Truffle.getRuntime().createDirectCallNode(target);
    }

    @ExplodeLoop
    private Object[] createArgumentsForCall(VirtualFrame frame, WasmFunction function, int numArgs, int stackPointer) {
        CompilerAsserts.partialEvaluationConstant(numArgs);
        Object[] args = new Object[numArgs];
        for (int i = numArgs - 1; i >= 0; --i) {
            stackPointer--;
            byte type = module().symbolTable().getFunctionTypeArgumentTypeAt(function.typeIndex(), i);
            switch (type) {
                case ValueTypes.I32_TYPE:
                    args[i] = popInt(frame, stackPointer);
                    break;
                case ValueTypes.I64_TYPE:
                    args[i] = pop(frame, stackPointer);
                    break;
                case ValueTypes.F32_TYPE:
                    args[i] = popAsFloat(frame, stackPointer);
                    break;
                case ValueTypes.F64_TYPE:
                    args[i] = popAsDouble(frame, stackPointer);
                    break;
            }
        }
        return args;
    }

    @ExplodeLoop
    private void unwindStack(VirtualFrame frame, int initStackPointer, int initialContinuationStackPointer, int targetBlockReturnLength) {
        // TODO: If the targetBlockReturnLength could ever be > 1, this would invert the stack
        // values.
        // The spec seems to imply that the operand stack should not be inverted.
        CompilerAsserts.partialEvaluationConstant(targetBlockReturnLength);
        int stackPointer = initStackPointer;
        int continuationStackPointer = initialContinuationStackPointer;
        for (int i = 0; i != targetBlockReturnLength; ++i) {
            stackPointer--;
            long value = pop(frame, stackPointer);
            push(frame, continuationStackPointer, value);
            continuationStackPointer++;
        }
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
       throw new WasmExecutionException(this, "This method should never have been called.");
    }

    @Override
    public Object executeRepeatingWithValue(VirtualFrame frame) {
        final TargetOffset offset = execute(contextReference().get(), frame);
        return offset;
    }

    @Override
    public boolean shouldContinue(Object value) {
        // This is a trick to avoid the load of the value field.
        // In particular, we avoid:
        //
        // return this.value == 0;
        //
        // This helps the partial evaluator short-circuit the
        // pattern with a diamond and a loop exit check,
        // when br_if occurs in the loop body.
        return value == TargetOffset.ZERO;
    }

    @Override
    public byte returnTypeId() {
        return returnTypeId;
    }

    public int continuationTypeLength() {
        return typeLength(continuationTypeId);
    }

}
