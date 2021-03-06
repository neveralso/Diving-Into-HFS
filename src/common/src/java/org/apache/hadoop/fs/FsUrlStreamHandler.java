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
package org.apache.hadoop.fs;

import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;

/**
 * URLStream handler relying on FileSystem and on a given Configuration to
 * handle URL protocols.
 * FsUrlStreamHandler类继承自URLStreamHandler,URLStreamHandler依赖于FileSystem类
 * 并且传入Configuration对象conf到其构造函数FsUrlStreamHandler(Configuration conf)
 * FsUrlStreamHandler拥有一个私有的实例对象 Configuration对象conf
 * FsUrlStreamHandler类的另一个构造函数是将其conf属性指向一个新的Configuration对象
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
class FsUrlStreamHandler extends URLStreamHandler {

  private Configuration conf;

  FsUrlStreamHandler(Configuration conf) {
    this.conf = conf;
  }

  FsUrlStreamHandler() {
    this.conf = new Configuration();
  }
/**
  * @param URL对象 url 统一资源标识符
  * @return FsUrlConnection对象
  * @throw IOException
  * 通过调用FsUrlConnection(conf, url)构造函数生成一个FsUrlConnection对象
  */
  @Override
  protected FsUrlConnection openConnection(URL url) throws IOException {
    return new FsUrlConnection(conf, url);
  }

}
