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
package org.apache.hadoop.fs.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于解析CLI命令参数并检查参数格式的工具类
 */
public class CommandFormat {
  final String name;
  final int minPar, maxPar;
  final Map<String, Boolean> options = new HashMap<String, Boolean>();

  /**
   * 构造函数
   *
   * @param n 命令名
   * @param min 最小参数数
   * @param max 最大参数数
   * @param possibleOpt 选项（形如 -xxx 的开关性参数，不计入参数数）
   */
  public CommandFormat(String n, int min, int max, String ... possibleOpt) {
    name = n;
    minPar = min;
    maxPar = max;
    for(String opt : possibleOpt)
      options.put(opt, Boolean.FALSE);
  }

  /**
   * 从给定位置开始解析命令参数
   * 
   * @param args 存储参数的数组
   * @param pos 参数起始位置
   * @return 参数列表
   */
  public List<String> parse(String[] args, int pos) {
    List<String> parameters = new ArrayList<String>();
    for(; pos < args.length; pos++) {
      if (args[pos].charAt(0) == '-' && args[pos].length() > 1) {
        String opt = args[pos].substring(1);
        if (options.containsKey(opt))
          options.put(opt, Boolean.TRUE);
        else
          throw new IllegalArgumentException("Illegal option " + args[pos]);
      }
      else
        parameters.add(args[pos]);
    }
    int psize = parameters.size();
    if (psize < minPar || psize > maxPar)
      throw new IllegalArgumentException("Illegal number of arguments");
    return parameters;
  }
  
  /**
   * 检查选项是否开启
   * 
   * @param option 选项
   * @return 开关状态
   */
  public boolean getOpt(String option) {
    return options.get(option);
  }
}