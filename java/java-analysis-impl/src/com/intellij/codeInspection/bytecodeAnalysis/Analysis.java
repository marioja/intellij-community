/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInspection.bytecodeAnalysis;

import com.intellij.codeInspection.bytecodeAnalysis.asm.ControlFlowGraph;
import com.intellij.codeInspection.bytecodeAnalysis.asm.DFSTree;
import com.intellij.codeInspection.bytecodeAnalysis.asm.RichControlFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame;

import java.util.*;

class AbstractValues {
  static final class ParamValue extends BasicValue {
    ParamValue(Type tp) {
      super(tp);
    }
  }
  static final BasicValue InstanceOfCheckValue = new BasicValue(Type.INT_TYPE) {
    @Override
    public boolean equals(Object value) {
      return this == value;
    }
  };

  static final BasicValue TrueValue = new BasicValue(Type.INT_TYPE) {
    @Override
    public boolean equals(Object value) {
      return this == value;
    }
  };

  static final BasicValue FalseValue = new BasicValue(Type.INT_TYPE) {
    @Override
    public boolean equals(Object value) {
      return this == value;
    }
  };

  static final BasicValue NullValue = new BasicValue(Type.getObjectType("null")) {
    @Override
    public boolean equals(Object value) {
      return this == value;
    }
  };
  static final class NotNullValue extends BasicValue {
    NotNullValue(Type tp) {
      super(tp);
    }
  }
  static final class CallResultValue extends BasicValue {
    final Set<Key> inters;
    CallResultValue(Type tp, Set<Key> inters) {
      super(tp);
      this.inters = inters;
    }
  }

  static final BasicValue CLASS_VALUE = new NotNullValue(Type.getObjectType("java/lang/Class"));
  static final BasicValue METHOD_VALUE = new NotNullValue(Type.getObjectType("java/lang/invoke/MethodType"));
  static final BasicValue STRING_VALUE = new NotNullValue(Type.getObjectType("java/lang/String"));
  static final BasicValue METHOD_HANDLE_VALUE = new NotNullValue(Type.getObjectType("java/lang/invoke/MethodHandle"));


  static boolean isInstance(Conf curr, Conf prev) {
    if (curr.insnIndex != prev.insnIndex) {
      return false;
    }
    Frame<BasicValue> currFr = curr.frame;
    Frame<BasicValue> prevFr = prev.frame;
    for (int i = 0; i < currFr.getLocals(); i++) {
      if (!isInstance(currFr.getLocal(i), prevFr.getLocal(i))) {
        return false;
      }
    }
    for (int i = 0; i < currFr.getStackSize(); i++) {
      if (!isInstance(currFr.getStack(i), prevFr.getStack(i))) {
        return false;
      }
    }
    return true;
  }

  static boolean isInstance(BasicValue curr, BasicValue prev) {
    if (prev instanceof ParamValue) {
      return curr instanceof ParamValue;
    }
    if (InstanceOfCheckValue == prev) {
      return InstanceOfCheckValue == curr;
    }
    if (TrueValue == prev) {
      return TrueValue == curr;
    }
    if (FalseValue == prev) {
      return FalseValue == curr;
    }
    if (NullValue == prev) {
      return NullValue == curr;
    }
    if (prev instanceof NotNullValue) {
      return curr instanceof NotNullValue;
    }
    if (prev instanceof CallResultValue) {
      if (curr instanceof CallResultValue) {
        CallResultValue prevCall = (CallResultValue) prev;
        CallResultValue currCall = (CallResultValue) curr;
        return prevCall.inters.equals(currCall.inters);
      }
      else {
        return false;
      }
    }
    return true;
  }

  static boolean equiv(Conf curr, Conf prev) {
    Frame<BasicValue> currFr = curr.frame;
    Frame<BasicValue> prevFr = prev.frame;
    for (int i = currFr.getStackSize() - 1; i >= 0; i--) {
      if (!equiv(currFr.getStack(i), prevFr.getStack(i))) {
        return false;
      }
    }
    for (int i = currFr.getLocals() - 1; i >= 0; i--) {
      if (!equiv(currFr.getLocal(i), prevFr.getLocal(i))) {
        return false;
      }
    }
    return true;
  }

  static boolean equiv(BasicValue curr, BasicValue prev) {
    if (curr.getClass() == prev.getClass()) {
      if (curr instanceof CallResultValue && prev instanceof CallResultValue) {
        Set<Key> keys1 = ((CallResultValue)prev).inters;
        Set<Key> keys2 = ((CallResultValue)curr).inters;
        return keys1.equals(keys2);
      }
      else return true;
    }
    else return false;
  }
}

final class Conf {
  final int insnIndex;
  final Frame<BasicValue> frame;
  final int fastHashCode;

  Conf(int insnIndex, Frame<BasicValue> frame) {
    this.insnIndex = insnIndex;
    this.frame = frame;

    int hash = 0;
    for (int i = 0; i < frame.getLocals(); i++) {
      hash = hash * 31 + frame.getLocal(i).getClass().hashCode();
    }
    for (int i = 0; i < frame.getStackSize(); i++) {
      hash = hash * 31 + frame.getStack(i).getClass().hashCode();
    }
    fastHashCode = hash;
  }
}

final class State {
  final int index;
  final Conf conf;
  final List<Conf> history;
  final boolean taken;
  final boolean hasCompanions;

  State(int index, Conf conf, List<Conf> history, boolean taken, boolean hasCompanions) {
    this.index = index;
    this.conf = conf;
    this.history = history;
    this.taken = taken;
    this.hasCompanions = hasCompanions;
  }
}

interface PendingAction<Res> {}
class ProceedState<Res> implements PendingAction<Res> {
  final State state;

  ProceedState(State state) {
    this.state = state;
  }
}
class MakeResult<Res> implements PendingAction<Res> {
  final State state;
  final Res subResult;
  final int[] indices;

  MakeResult(State state, Res subResult, int[] indices) {
    this.state = state;
    this.subResult = subResult;
    this.indices = indices;
  }
}

abstract class Analysis<Res> {
  private static final ThreadLocal<PendingAction[]> ourPending = new ThreadLocal<PendingAction[]>() {
    @Override
    protected PendingAction[] initialValue() {
      return new PendingAction[Analysis.STEPS_LIMIT];
    }
  };

  public static final int STEPS_LIMIT = 30000;
  public static final int EQUATION_SIZE_LIMIT = 30;
  public static final int MERGE_LIMIT = 100000;
  final RichControlFlow richControlFlow;
  final Direction direction;
  final ControlFlowGraph controlFlow;
  final MethodNode methodNode;
  final Method method;
  final DFSTree dfsTree;
  final Res myIdentity;

  final private PendingAction<Res>[] pending = ourPending.get();
  final protected List<State>[] computed;
  final protected Res[] results;
  final Key aKey;

  Res earlyResult = null;
  Res internalResult = null;

  abstract Res identity();
  abstract Res combineResults(Res delta, int[] subResults) throws AnalyzerException;
  abstract boolean isEarlyResult(Res res);
  @NotNull
  abstract Equation<Key, Value> mkEquation(Res result);
  abstract void processState(State state) throws AnalyzerException;

  protected Analysis(RichControlFlow richControlFlow, Direction direction, boolean stable, Res[] results) {
    this.richControlFlow = richControlFlow;
    this.direction = direction;
    this.results = results;
    controlFlow = richControlFlow.controlFlow;
    methodNode = controlFlow.methodNode;
    method = new Method(controlFlow.className, methodNode.name, methodNode.desc);
    dfsTree = richControlFlow.dfsTree;
    aKey = new Key(method, direction, stable);
    computed = (List<State>[]) new List[controlFlow.transitions.length];
    myIdentity = identity();
  }

  final State createStartState() {
    return new State(0, new Conf(0, createStartFrame()), new ArrayList<Conf>(), false, false);
  }

  static boolean stateEquiv(State curr, State prev) {
    if (curr.taken != prev.taken) {
      return false;
    }
    if (curr.conf.fastHashCode != prev.conf.fastHashCode) {
      return false;
    }
    if (!AbstractValues.equiv(curr.conf, prev.conf)) {
      return false;
    }
    if (curr.history.size() != prev.history.size()) {
      return false;
    }
    for (int i = 0; i < curr.history.size(); i++) {
      Conf curr1 = curr.history.get(i);
      Conf prev1 = prev.history.get(i);
      if (curr1.fastHashCode != prev1.fastHashCode || !AbstractValues.equiv(curr1, prev1)) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  final Equation<Key, Value> analyze() throws AnalyzerException {
    pendingPush(new ProceedState<Res>(createStartState()));
    int steps = 0;
    while (pendingTop > 0 && earlyResult == null) {
      steps ++;
      if (steps >= STEPS_LIMIT) {
        throw new AnalyzerException(null, "limit is reached, steps: " + steps + " in method " + method);
      }
      PendingAction<Res> action = pending[--pendingTop];
      if (action instanceof MakeResult) {
        MakeResult<Res> makeResult = (MakeResult<Res>) action;
        Res result = combineResults(makeResult.subResult, makeResult.indices);
        if (isEarlyResult(result)) {
          earlyResult = result;
        } else {
          State state = makeResult.state;
          int insnIndex = state.conf.insnIndex;
          results[state.index] = result;
          addComputed(insnIndex, state);
        }
      }
      else if (action instanceof ProceedState) {
        ProceedState<Res> proceedState = (ProceedState<Res>) action;
        State state = proceedState.state;
        int insnIndex = state.conf.insnIndex;
        Conf conf = state.conf;
        List<Conf> history = state.history;

        boolean fold = false;
        if (dfsTree.loopEnters[insnIndex]) {
          for (Conf prev : history) {
            if (AbstractValues.isInstance(conf, prev)) {
              fold = true;
              break;
            }
          }
        }
        if (fold) {
          results[state.index] = myIdentity;
          addComputed(insnIndex, state);
        }
        else {
          State baseState = null;
          List<State> thisComputed = computed[insnIndex];
          if (thisComputed != null) {
            for (State prevState : thisComputed) {
              if (stateEquiv(state, prevState)) {
                baseState = prevState;
                break;
              }
            }
          }
          if (baseState != null) {
            results[state.index] = results[baseState.index];
          } else {
            // the main call
            processState(state);
          }

        }
      }
    }
    if (earlyResult != null) {
      return mkEquation(earlyResult);
    } else if (internalResult != null) {
      return mkEquation(internalResult);
    } else {
      return mkEquation(results[0]);
    }
  }

  final Frame<BasicValue> createStartFrame() {
    Frame<BasicValue> frame = new Frame<BasicValue>(methodNode.maxLocals, methodNode.maxStack);
    Type returnType = Type.getReturnType(methodNode.desc);
    BasicValue returnValue = Type.VOID_TYPE.equals(returnType) ? null : new BasicValue(returnType);
    frame.setReturn(returnValue);

    Type[] args = Type.getArgumentTypes(methodNode.desc);
    int local = 0;
    if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
      frame.setLocal(local++, new AbstractValues.NotNullValue(Type.getObjectType(controlFlow.className)));
    }
    for (int i = 0; i < args.length; i++) {
      BasicValue value;
      if (direction instanceof InOut && ((InOut)direction).paramIndex == i) {
        value = new AbstractValues.ParamValue(args[i]);
      }
      else if (direction instanceof In && ((In)direction).paramIndex == i) {
        value = new AbstractValues.ParamValue(args[i]);
      }
      else {
        value = new BasicValue(args[i]);
      }
      frame.setLocal(local++, value);
      if (args[i].getSize() == 2) {
        frame.setLocal(local++, BasicValue.UNINITIALIZED_VALUE);
      }
    }
    while (local < methodNode.maxLocals) {
      frame.setLocal(local++, BasicValue.UNINITIALIZED_VALUE);
    }
    return frame;
  }

  private int pendingTop = 0;

  protected final void pendingPush(PendingAction<Res> action) throws AnalyzerException {
    if (pendingTop >= STEPS_LIMIT) {
        throw new AnalyzerException(null, "limit is reached in method " + method);
    }
    pending[pendingTop++] = action;
  }

  static BasicValue popValue(Frame<BasicValue> frame) {
    return frame.getStack(frame.getStackSize() - 1);
  }

  static <A> List<A> append(List<A> xs, A x) {
    ArrayList<A> result = new ArrayList<A>();
    if (xs != null) {
      result.addAll(xs);
    }
    result.add(x);
    return result;
  }

  protected void addComputed(int i, State s) {
    List<State> states = computed[i];
    if (states == null) {
      states = new ArrayList<State>();
      computed[i] = states;
    }
    states.add(s);
  }
}
