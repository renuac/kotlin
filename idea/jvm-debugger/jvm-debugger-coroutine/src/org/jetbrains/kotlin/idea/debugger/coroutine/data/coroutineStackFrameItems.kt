/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.LocationStackFrameProxyImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.util.findPosition
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isInvokeSuspend
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.stackFrame.KotlinStackFrame

/**
 * Creation frame of coroutine either in RUNNING or SUSPENDED state.
 */
class CreationCoroutineStackFrameItem(
    val stackTraceElement: StackTraceElement,
    location: Location,
    val first: Boolean
) : CoroutineStackFrameItem(location, emptyList()) {

    override fun createFrame(debugProcess: DebugProcessImpl): CoroutineGeneratedFrame? {
        return debugProcess.invokeInManagerThread {
            val frame = debugProcess.findFirstFrame() ?: return@invokeInManagerThread null
            val locationFrame = LocationStackFrameProxyImpl(location, frame)
            val position = location.findPosition(debugProcess.project)
            CreationCoroutineStackFrame(debugProcess, this, first)
        }
    }
}

/**
 * Restored frame in SUSPENDED coroutine, not attached to any thread.
 */
class SuspendCoroutineStackFrameItem(
    val stackTraceElement: StackTraceElement,
    location: Location,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(location, spilledVariables)

/**
 * Restored from memory dump
 */
class DefaultCoroutineStackFrameItem(location: Location, spilledVariables: List<XNamedValue>) :
    CoroutineStackFrameItem(location, spilledVariables) {

    override fun createFrame(debugProcess: DebugProcessImpl): CoroutineGeneratedFrame? {
        return debugProcess.invokeInManagerThread {
            val frame = debugProcess.findFirstFrame() ?: return@invokeInManagerThread null
            val locationStackFrameProxyImpl = LocationStackFrameProxyImpl(location, frame)
            val position = location.findPosition(debugProcess.project) ?: return@invokeInManagerThread null
            CoroutineStackFrame(debugProcess, this)
        }
    }
}

/**
 * Original frame appeared before resumeWith call.
 *
 * Sequence is the following
 *
 * - KotlinStackFrame
 * - invokeSuspend(KotlinStackFrame) -|
 *                                    | replaced with CoroutinePreflightStackFrame
 * - resumeWith(KotlinStackFrame) ----|
 * - Kotlin/JavaStackFrame -> PreCoroutineStackFrameItem : CoroutinePreflightStackFrame.threadPreCoroutineFrames
 *
 */
open class RunningCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(frame.location(), spilledVariables), FrameProvider {
    override fun createFrame(debugProcess: DebugProcessImpl): CoroutineGeneratedFrame? {
        return debugProcess.invokeInManagerThread {
            CoroutineStackFrame(debugProcess, this)
        }
    }

    override fun provideFrame(debugProcess: DebugProcessImpl): XStackFrame? =
        debugProcess.invokeInManagerThread { KotlinStackFrame(frame) }
}

sealed class CoroutineStackFrameItem(val location: Location, val spilledVariables: List<XNamedValue>) :
    StackFrameItem(location, spilledVariables) {
    val log by logger

    override fun createFrame(debugProcess: DebugProcessImpl): CoroutineGeneratedFrame? {
        return debugProcess.invokeInManagerThread {
            CoroutineStackFrame(debugProcess, this)
        }
    }

    fun uniqueId() =
        location.safeSourceName() + ":" + location.safeMethod().toString() + ":" +
                location.safeLineNumber() + ":" + location.safeKotlinPreferredLineNumber()

    fun isInvokeSuspend(): Boolean =
        location.isInvokeSuspend()
}

interface FrameProvider {
    fun provideFrame(debugProcess: DebugProcessImpl): XStackFrame?
}

fun DebugProcessImpl.findFirstFrame(): StackFrameProxyImpl? =
    suspendManager.pausedContext.thread?.forceFrames()?.firstOrNull()
