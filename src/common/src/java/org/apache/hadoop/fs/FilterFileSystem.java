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

import java.io.*;
import java.net.URI;
import java.util.EnumSet;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

/****************************************************************
 * FilterFileSystem是一个代理, 或者说, wrapper,
 * 它的成员属性包含一个{@link FileSystem}实例fs,
 * 把fs的所有protected方法导出成了public方法.
 *
 * Filter意为过滤器, {@link FilterFileSystem}选择的过滤策略是"直通",
 * 即什么都不做, 直接把参数传递给public方法对应的protected方法.
 * <code>FilterFileSystem</code> contains
 * some other file system, which it uses as
 * its  basic file system, possibly transforming
 * the data along the way or providing  additional
 * functionality. The class <code>FilterFileSystem</code>
 * itself simply overrides all  methods of
 * <code>FileSystem</code> with versions that
 * pass all requests to the contained  file
 * system. Subclasses of <code>FilterFileSystem</code>
 * may further override some of  these methods
 * and may also provide additional methods
 * and fields.
 *
 * FilterFileSystem继承了FileSystem类，重写了父类的所有方法 
 * 它转换数据或者增加方法，加以封装，相当于起到了过滤文件系统的作用。
 * FilterFileSystem包装了一个FileSystem对象，所有提供的方法都是与
 * FileSystem中相同的方法，通过调用FileSystem对象对应的函数来实现。
 * 此类的目的是通过继承此类来重写其中的方法，来添加更多地功能
 *
 *****************************************************************/
@InterfaceAudience.Public
@InterfaceStability.Stable
public class FilterFileSystem extends FileSystem {

  protected FileSystem fs;

  /*
   * so that extending classes can define it
   */
  public FilterFileSystem() {
  }

  public FilterFileSystem(FileSystem fs) {
    this.fs = fs;
    this.statistics = fs.statistics;
  }

  /** Called after a new FileSystem instance is constructed.
   * @param name a uri whose authority section names the host, port, etc.
   *   for this FileSystem
   * @param conf the configuration
   */
  public void initialize(URI name, Configuration conf) throws IOException {
    fs.initialize(name, conf);
  }

  /** Returns a URI whose scheme and authority identify this FileSystem.*/
  public URI getUri() {
    return fs.getUri();
  }

  /** Make sure that a path specifies a FileSystem. */
  public Path makeQualified(Path path) {
    return fs.makeQualified(path);
  }

  ///////////////////////////////////////////////////////////////
  // FileSystem
  ///////////////////////////////////////////////////////////////

  /** Check that a Path belongs to this FileSystem. */
  protected void checkPath(Path path) {
    fs.checkPath(path);
  }

  public BlockLocation[] getFileBlockLocations(FileStatus file, long start,
    long len) throws IOException {
      return fs.getFileBlockLocations(file, start, len);
  }

  /**
   * Opens an FSDataInputStream at the indicated Path.
   * @param f the file name to open
   * @param bufferSize the size of the buffer to be used.
   */
  public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    return fs.open(f, bufferSize);
  }

  /** {@inheritDoc} */
  public FSDataOutputStream append(Path f, int bufferSize,
      Progressable progress) throws IOException {
    return fs.append(f, bufferSize, progress);
  }

  /** {@inheritDoc} */
  @Override
  public FSDataOutputStream create(Path f, FsPermission permission,
      boolean overwrite, int bufferSize, short replication, long blockSize,
      Progressable progress) throws IOException {
    return fs.create(f, permission,
        overwrite, bufferSize, replication, blockSize, progress);
  }

  /**
   * Set replication for an existing file.
   *
   * @param src file name
   * @param replication new replication
   * @throws IOException
   * @return true if successful;
   *         false if file does not exist or is a directory
   */
  public boolean setReplication(Path src, short replication) throws IOException {
    return fs.setReplication(src, replication);
  }

  /**
   * Renames Path src to Path dst.  Can take place on local fs
   * or remote DFS.
   */
  public boolean rename(Path src, Path dst) throws IOException {
    return fs.rename(src, dst);
  }

  /** Delete a file */
  public boolean delete(Path f, boolean recursive) throws IOException {
    return fs.delete(f, recursive);
  }

  /**
   * Mark a path to be deleted when FileSystem is closed.
   * When the JVM shuts down,
   * all FileSystem objects will be closed automatically.
   * Then,
   * the marked path will be deleted as a result of closing the FileSystem.
   *
   * The path has to exist in the file system.
   *
   * @param f the path to delete.
   * @return  true if deleteOnExit is successful, otherwise false.
   * @throws IOException
   */
  public boolean deleteOnExit(Path f) throws IOException {
    return fs.deleteOnExit(f);
  }

  /** List files in a directory. */
  public FileStatus[] listStatus(Path f) throws IOException {
    return fs.listStatus(f);
  }

  public Path getHomeDirectory() {
    return fs.getHomeDirectory();
  }


  /**
   * Set the current working directory for the given file system. All relative
   * paths will be resolved relative to it.
   *
   * @param newDir
   */
  public void setWorkingDirectory(Path newDir) {
    fs.setWorkingDirectory(newDir);
  }

  /**
   * Get the current working directory for the given file system
   *
   * @return the directory pathname
   */
  public Path getWorkingDirectory() {
    return fs.getWorkingDirectory();
  }

  protected Path getInitialWorkingDirectory() {
    return fs.getInitialWorkingDirectory();
  }

  /** {@inheritDoc} */
  @Override
  public FsStatus getStatus(Path p) throws IOException {
    return fs.getStatus(p);
  }

  /** {@inheritDoc} */
  @Override
  public boolean mkdirs(Path f, FsPermission permission) throws IOException {
    return fs.mkdirs(f, permission);
  }

  /**
   * The src file is on the local disk.  Add it to FS at
   * the given dst name.
   * delSrc indicates if the source should be removed
   */
  public void copyFromLocalFile(boolean delSrc, Path src, Path dst)
    throws IOException {
    fs.copyFromLocalFile(delSrc, src, dst);
  }

  /**
   * The src files are on the local disk.  Add it to FS at
   * the given dst name.
   * delSrc indicates if the source should be removed
   */
  public void copyFromLocalFile(boolean delSrc, boolean overwrite,
                                Path[] srcs, Path dst)
    throws IOException {
    fs.copyFromLocalFile(delSrc, overwrite, srcs, dst);
  }

  /**
   * The src file is on the local disk.  Add it to FS at
   * the given dst name.
   * delSrc indicates if the source should be removed
   */
  public void copyFromLocalFile(boolean delSrc, boolean overwrite,
                                Path src, Path dst)
    throws IOException {
    fs.copyFromLocalFile(delSrc, overwrite, src, dst);
  }

  /**
   * The src file is under FS, and the dst is on the local disk.
   * Copy it from FS control to the local dst name.
   * delSrc indicates if the src will be removed or not.
   */
  public void copyToLocalFile(boolean delSrc, Path src, Path dst)
    throws IOException {
    fs.copyToLocalFile(delSrc, src, dst);
  }

  /**
   * Returns a local File that the user can write output to.  The caller
   * provides both the eventual FS target name and the local working
   * file.  If the FS is local, we write directly into the target.  If
   * the FS is remote, we write into the tmp local area.
   */
  public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile)
    throws IOException {
    return fs.startLocalOutput(fsOutputFile, tmpLocalFile);
  }

  /**
   * Called when we're all done writing to the target.  A local FS will
   * do nothing, because we've written to exactly the right place.  A remote
   * FS will copy the contents of tmpLocalFile to the correct target at
   * fsOutputFile.
   */
  public void completeLocalOutput(Path fsOutputFile, Path tmpLocalFile)
    throws IOException {
    fs.completeLocalOutput(fsOutputFile, tmpLocalFile);
  }

  /** Return the total size of all files in the filesystem.*/
  public long getUsed() throws IOException{
    return fs.getUsed();
  }

  /** Return the number of bytes that large input files should be optimally
   * be split into to minimize i/o time. */
  public long getDefaultBlockSize() {
    return fs.getDefaultBlockSize();
  }

  /**
   * Get the default replication.
   */
  public short getDefaultReplication() {
    return fs.getDefaultReplication();
  }

  /**
   * Get file status.
   */
  public FileStatus getFileStatus(Path f) throws IOException {
    return fs.getFileStatus(f);
  }

  /** {@inheritDoc} */
  public FileChecksum getFileChecksum(Path f) throws IOException {
    return fs.getFileChecksum(f);
  }

  /** {@inheritDoc} */
  public void setVerifyChecksum(boolean verifyChecksum) {
    fs.setVerifyChecksum(verifyChecksum);
  }

  @Override
  public Configuration getConf() {
    return fs.getConf();
  }

  @Override
  public void close() throws IOException {
    super.close();
    fs.close();
  }

  /** {@inheritDoc} */
  @Override
  public void setOwner(Path p, String username, String groupname
      ) throws IOException {
    fs.setOwner(p, username, groupname);
  }

  /** {@inheritDoc} */
  @Override
  public void setTimes(Path p, long mtime, long atime
      ) throws IOException {
    fs.setTimes(p, mtime, atime);
  }

  /** {@inheritDoc} */
  @Override
  public void setPermission(Path p, FsPermission permission
      ) throws IOException {
    fs.setPermission(p, permission);
  }

  @Override
  protected FSDataOutputStream primitiveCreate(Path f,
      FsPermission absolutePermission, EnumSet<CreateFlag> flag,
      int bufferSize, short replication, long blockSize, Progressable progress, int bytesPerChecksum)
      throws IOException {
    return fs.primitiveCreate(f, absolutePermission, flag,
        bufferSize, replication, blockSize, progress, bytesPerChecksum);
  }

  @Override
  protected boolean primitiveMkdir(Path f, FsPermission abdolutePermission)
      throws IOException {
    return fs.primitiveMkdir(f, abdolutePermission);
  }
}
