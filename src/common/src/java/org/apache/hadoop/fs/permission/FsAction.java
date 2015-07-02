/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.fs.permission;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * 将全线模式映射到Unix风格的表示,
 * 管理文件系统的行为：读、写、可运行等。
 */
@InterfaceAudience.LimitedPrivate({"HDFS"})
@InterfaceStability.Unstable
public enum FsAction {
  // POSIX style
  NONE("---"),
  EXECUTE("--x"),
  WRITE("-w-"),
  WRITE_EXECUTE("-wx"),
  READ("r--"),
  READ_EXECUTE("r-x"),
  READ_WRITE("rw-"),
  ALL("rwx");

  /** Retain reference to value array. */
  private final static FsAction[] vals = values();

  /** Symbolic representation */
  public final String SYMBOL;

  private FsAction(String s) {
    SYMBOL = s;
  }

  /**
   * Return true if this action implies that action.
   * 如果这个行为隐含着那个行为，返回真
   * @param that
   */
  public boolean implies(FsAction that) {
    if (that != null) {
      return (ordinal() & that.ordinal()) == that.ordinal();
    }
    return false;
  }

  /** AND operation. */
  /**
   * 行为的与操作
   * @param that
   * @return
   */
  public FsAction and(FsAction that) {
    return vals[ordinal() & that.ordinal()];
  }
  /** OR operation.
   * 行为的或操作
   * */
  public FsAction or(FsAction that) {
    return vals[ordinal() | that.ordinal()];
  }
  /** NOT operation.
   * 行为的非操作
   * */
  public FsAction not() {
    return vals[7 - ordinal()];
  }
}
