/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JVMStackFrameInfoProvider
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import org.jetbrains.kotlin.idea.debugger.coroutine.KotlinDebuggerCoroutinesBundle
import org.jetbrains.kotlin.idea.debugger.stackFrame.KotlinStackFrame

/**
 * Coroutine exit frame represented by a stack frames
 * invokeSuspend():-1
 * resumeWith()
 *
 */
class CoroutinePreflightFrame(
    val coroutineInfoData: CoroutineInfoData,
    private val frame: StackFrameProxyImpl,
    val threadPreCoroutineFrames: List<StackFrameProxyImpl>,
    val mode: SuspendExitMode,
    private val firstFrameVariables: List<XNamedValue> = coroutineInfoData.topFrameVariables()
) : KotlinStackFrame(frame), JVMStackFrameInfoProvider {

    override fun superBuildVariables(evaluationContext: EvaluationContextImpl, children: XValueChildrenList) {
        super.superBuildVariables(evaluationContext, children)
        children.let {
            val varNames = (0 until children.size()).map { children.getName(it) }.toSet()
            firstFrameVariables.forEach {
                if (!varNames.contains(it.name))
                    children.add(it)
            }
        }
    }

    override fun isInLibraryContent() = false

    override fun isSynthetic() = false

    fun restoredStackTrace() =
        coroutineInfoData.restoredStackTrace(mode)
}

class CreationCoroutineStackFrame(debugProcess: DebugProcessImpl, item: CoroutineStackFrameItem, val first: Boolean) : CoroutineStackFrame(debugProcess, item) {
    override fun getCaptionAboveOf() = KotlinDebuggerCoroutinesBundle.message("coroutine.dump.creation.trace")

    override fun hasSeparatorAbove(): Boolean =
        first
}

open class CoroutineStackFrame(val debugProcess: DebugProcessImpl, val item: CoroutineStackFrameItem) :
    StackFrameItem.CapturedStackFrame(debugProcess, item) {

    override fun computeChildren(node: XCompositeNode) {
        if (item is FrameProvider)
            item.provideFrame(debugProcess)?.computeChildren(node)
        else
            super.computeChildren(node)
    }

    override fun getCaptionAboveOf() = "CoroutineExit"

    override fun hasSeparatorAbove(): Boolean =
        false
}

typealias CoroutineGeneratedFrame = StackFrameItem.CapturedStackFrame