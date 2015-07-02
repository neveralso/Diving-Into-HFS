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

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.StringTokenizer;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.util.StringUtils;

/****************************************************************
 * 该类实现了一个原生本地文件系统。可以作为一个具有最基本功能的文件系统。
 *****************************************************************/
@InterfaceAudience.Public
@InterfaceStability.Stable
public class RawLocalFileSystem extends FileSystem {
  /**
   * 本地文件名的URI
   * file://来标识该文件系统为本地文件系统
   */
  static final URI NAME = URI.create("file:///");
  /**
   * 用户的当前工作目录
   */
  private Path workingDir;

  /**
   * 在构造方法中完成对用户的当前工作目录的初始化
   */
  public RawLocalFileSystem() {
    workingDir = getInitialWorkingDirectory();
  }

  /**
   * Convert a path to a File.
   * 用于将一个路径转化为以用户工作目录为父目录的绝对路径
   */
  public File pathToFile(Path path) {
    checkPath(path);
    if (!path.isAbsolute()) {
      path = new Path(getWorkingDirectory(), path);
    }
    return new File(path.toUri().getPath());
  }

  /**
   * 获取本地文件名的URI
   * @return
   */
  public URI getUri() { return NAME; }

  /**
   * 初始化
   * @param uri
   * @param conf the configuration
   * @throws IOException
   */
  public void initialize(URI uri, Configuration conf) throws IOException {
    super.initialize(uri, conf);
    setConf(conf);
  }

  /**
   * 本地文件系统的读取操作
   * 会将读到的字节数添加到统计信息中
   */
  class TrackingFileInputStream extends FileInputStream {
    public TrackingFileInputStream(File f) throws IOException {
      super(f);
    }

    public int read() throws IOException {
      int result = super.read();
      if (result != -1) {
        statistics.incrementBytesRead(1);
      }
      return result;
    }

    public int read(byte[] data) throws IOException {
      int result = super.read(data);
      if (result != -1) {
        statistics.incrementBytesRead(result);
      }
      return result;
    }

    public int read(byte[] data, int offset, int length) throws IOException {
      int result = super.read(data, offset, length);
      if (result != -1) {
        statistics.incrementBytesRead(result);
      }
      return result;
    }
  }

  /*******************************************************
   * For open()'s FSInputStream.
   *******************************************************/
  /**
   * 依靠包装的FileInputStream的实例进行读取操作
   * 通过 TrackingFileInputStream来初始化
   */
  class LocalFSFileInputStream extends FSInputStream {
    private FileInputStream fis;
    private long position;

    public LocalFSFileInputStream(Path f) throws IOException {
      this.fis = new TrackingFileInputStream(pathToFile(f));
    }

    public void seek(long pos) throws IOException {
      fis.getChannel().position(pos);
      this.position = pos;
    }

    public long getPos() throws IOException {
      return this.position;
    }

    public boolean seekToNewSource(long targetPos) throws IOException {
      return false;
    }

    /*
     * Just forward to the fis
     */
    public int available() throws IOException { return fis.available(); }
    public void close() throws IOException { fis.close(); }
    public boolean markSupport() { return false; }

    public int read() throws IOException {
      try {
        int value = fis.read();
        if (value >= 0) {
          this.position++;
        }
        return value;
      } catch (IOException e) {                 // unexpected exception
        throw new FSError(e);                   // assume native fs error
      }
    }

    public int read(byte[] b, int off, int len) throws IOException {
      try {
        int value = fis.read(b, off, len);
        if (value > 0) {
          this.position += value;
        }
        return value;
      } catch (IOException e) {                 // unexpected exception
        throw new FSError(e);                   // assume native fs error
      }
    }

    public int read(long position, byte[] b, int off, int len)
      throws IOException {
      ByteBuffer bb = ByteBuffer.wrap(b, off, len);
      try {
        return fis.getChannel().read(bb, position);
      } catch (IOException e) {
        throw new FSError(e);
      }
    }

    public long skip(long n) throws IOException {
      long value = fis.skip(n);
      if (value > 0) {
        this.position += value;
      }
      return value;
    }
  }

  public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    if (!exists(f)) {
      throw new FileNotFoundException(f.toString());
    }
    return new FSDataInputStream(new BufferedFSInputStream(
        new LocalFSFileInputStream(f), bufferSize));
  }

  /*********************************************************
   * For create()'s FSOutputStream.
   * 同输入流，依靠包装的FileOutputStream的实例进行写入操作
   *********************************************************/
  class LocalFSFileOutputStream extends OutputStream {
    private FileOutputStream fos;

    private LocalFSFileOutputStream(Path f, boolean append) throws IOException {
      this.fos = new FileOutputStream(pathToFile(f), append);
    }

    /*
     * Just forward to the fos
     */
    public void close() throws IOException { fos.close(); }
    public void flush() throws IOException { fos.flush(); }
    public void write(byte[] b, int off, int len) throws IOException {
      try {
        fos.write(b, off, len);
      } catch (IOException e) {                // unexpected exception
        throw new FSError(e);                  // assume native fs error
      }
    }

    public void write(int b) throws IOException {
      try {
        fos.write(b);
      } catch (IOException e) {              // unexpected exception
        throw new FSError(e);                // assume native fs error
      }
    }
  }

  /** {@inheritDoc} */
  public FSDataOutputStream append(Path f, int bufferSize,
      Progressable progress) throws IOException {
    if (!exists(f)) {
      throw new FileNotFoundException("File " + f + " not found.");
    }
    if (getFileStatus(f).isDirectory()) {
      throw new IOException("Cannot append to a diretory (=" + f + " ).");
    }
    return new FSDataOutputStream(new BufferedOutputStream(
        new LocalFSFileOutputStream(f, true), bufferSize), statistics);
  }

  /** {@inheritDoc} */
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize,
    short replication, long blockSize, Progressable progress)
    throws IOException {
    if (exists(f) && !overwrite) {
      throw new IOException("File already exists:"+f);
    }
    Path parent = f.getParent();
    if (parent != null && !mkdirs(parent)) {
      throw new IOException("Mkdirs failed to create " + parent.toString());
    }
    return new FSDataOutputStream(new BufferedOutputStream(
        new LocalFSFileOutputStream(f, false), bufferSize), statistics);
  }

  /** {@inheritDoc} */
  @Override
  public FSDataOutputStream create(Path f, FsPermission permission,
    boolean overwrite, int bufferSize, short replication, long blockSize,
    Progressable progress) throws IOException {

    FSDataOutputStream out = create(f,
        overwrite, bufferSize, replication, blockSize, progress);
    setPermission(f, permission);
    return out;
  }


  @Override
  protected FSDataOutputStream primitiveCreate(Path f,
      FsPermission absolutePermission, EnumSet<CreateFlag> flag,
      int bufferSize, short replication, long blockSize, Progressable progress,
      int bytesPerChecksum) throws IOException {

    if(flag.contains(CreateFlag.APPEND)){
      if (!exists(f)){
        if(flag.contains(CreateFlag.CREATE)) {
          return create(f, false, bufferSize, replication, blockSize, null);
        }
      }
      return append(f, bufferSize, null);
    }

    FSDataOutputStream out = create(f, flag.contains(CreateFlag.OVERWRITE),
                                 bufferSize, replication, blockSize, progress);
    setPermission(f, absolutePermission);
    return out;
  }

  public boolean rename(Path src, Path dst) throws IOException {
    if (pathToFile(src).renameTo(pathToFile(dst))) {
      return true;
    }
    return FileUtil.copy(this, src, this, dst, true, getConf());
  }

  public boolean delete(Path p, boolean recursive) throws IOException {
    File f = pathToFile(p);
    if (f.isFile()) {
      return f.delete();
    } else if ((!recursive) && f.isDirectory() &&
        (f.listFiles().length != 0)) {
      throw new IOException("Directory " + f.toString() + " is not empty");
    }
    return FileUtil.fullyDelete(f);
  }

  public FileStatus[] listStatus(Path f) throws IOException {
    File localf = pathToFile(f);
    FileStatus[] results;

    if (!localf.exists()) {
      throw new FileNotFoundException("File " + f + " does not exist.");
    }
    if (localf.isFile()) {
      return new FileStatus[] {
        new RawLocalFileStatus(localf, getDefaultBlockSize(), this) };
    }

    String[] names = localf.list();
    if (names == null) {
      return null;
    }
    results = new FileStatus[names.length];
    for (int i = 0; i < names.length; i++) {
      results[i] = getFileStatus(new Path(f, names[i]));
    }
    return results;
  }

  /**
   * Creates the specified directory hierarchy. Does not
   * treat existence as an error.
   */
  public boolean mkdirs(Path f) throws IOException {
    if(f == null) {
      throw new IllegalArgumentException("mkdirs path arg is null");
    }
    Path parent = f.getParent();
    File p2f = pathToFile(f);
    if(parent != null) {
      File parent2f = pathToFile(parent);
      if(parent2f != null && parent2f.exists() && !parent2f.isDirectory()) {
        throw new FileAlreadyExistsException("Parent path is not a directory: "
            + parent);
      }
    }
    return (parent == null || mkdirs(parent)) &&
      (p2f.mkdir() || p2f.isDirectory());
  }

  /** {@inheritDoc} */
  @Override
  public boolean mkdirs(Path f, FsPermission permission) throws IOException {
    boolean b = mkdirs(f);
    if(b) {
      setPermission(f, permission);
    }
    return b;
  }


  @Override
  protected boolean primitiveMkdir(Path f, FsPermission absolutePermission)
    throws IOException {
    boolean b = mkdirs(f);
    setPermission(f, absolutePermission);
    return b;
  }


  @Override
  public Path getHomeDirectory() {
    return this.makeQualified(new Path(System.getProperty("user.home")));
  }

  /**
   * Set the working directory to the given directory.
   */
  @Override
  public void setWorkingDirectory(Path newDir) {
    workingDir = newDir;
  }

  @Override
  public Path getWorkingDirectory() {
    return workingDir;
  }

  @Override
  protected Path getInitialWorkingDirectory() {
    return this.makeQualified(new Path(System.getProperty("user.dir")));
  }

  /** {@inheritDoc} */
  @Override
  public FsStatus getStatus(Path p) throws IOException {
    File partition = pathToFile(p == null ? new Path("/") : p);
    //File provides getUsableSpace() and getFreeSpace()
    //File provides no API to obtain used space, assume used = total - free
    return new FsStatus(partition.getTotalSpace(),
      partition.getTotalSpace() - partition.getFreeSpace(),
      partition.getFreeSpace());
  }

  // In the case of the local filesystem, we can just rename the file.
  public void moveFromLocalFile(Path src, Path dst) throws IOException {
    rename(src, dst);
  }

  // We can write output directly to the final location
  public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile)
    throws IOException {
    return fsOutputFile;
  }

  // It's in the right place - nothing to do.
  public void completeLocalOutput(Path fsWorkingFile, Path tmpLocalFile)
    throws IOException {
  }

  public void close() throws IOException {
    super.close();
  }

  public String toString() {
    return "LocalFS";
  }

  public FileStatus getFileStatus(Path f) throws IOException {
    File path = pathToFile(f);
    if (path.exists()) {
      return new RawLocalFileStatus(pathToFile(f), getDefaultBlockSize(), this);
    } else {
      throw new FileNotFoundException("File " + f + " does not exist.");
    }
  }

  static class RawLocalFileStatus extends FileStatus {
    /* We can add extra fields here. It breaks at least CopyFiles.FilePair().
     * We recognize if the information is already loaded by check if
     * onwer.equals("").
     */
    private boolean isPermissionLoaded() {
      return !super.getOwner().equals("");
    }

    RawLocalFileStatus(File f, long defaultBlockSize, FileSystem fs) {
      super(f.length(), f.isDirectory(), 1, defaultBlockSize,
            f.lastModified(), fs.makeQualified(new Path(f.getPath())));
    }

    @Override
    public FsPermission getPermission() {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      return super.getPermission();
    }

    @Override
    public String getOwner() {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      return super.getOwner();
    }

    @Override
    public String getGroup() {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      return super.getGroup();
    }

    /// loads permissions, owner, and group from `ls -ld`
    private void loadPermissionInfo() {
      IOException e = null;
      try {
        StringTokenizer t = new StringTokenizer(
            execCommand(new File(getPath().toUri()),
                        Shell.getGET_PERMISSION_COMMAND()));
        //expected format
        //-rw-------    1 username groupname ...
        String permission = t.nextToken();
        if (permission.length() > 10) { //files with ACLs might have a '+'
          permission = permission.substring(0, 10);
        }
        setPermission(FsPermission.valueOf(permission));
        t.nextToken();
        setOwner(t.nextToken());
        setGroup(t.nextToken());
      } catch (Shell.ExitCodeException ioe) {
        if (ioe.getExitCode() != 1) {
          e = ioe;
        } else {
          setPermission(null);
          setOwner(null);
          setGroup(null);
        }
      } catch (IOException ioe) {
        e = ioe;
      } finally {
        if (e != null) {
          throw new RuntimeException("Error while running command to get " +
                                     "file permissions : " +
                                     StringUtils.stringifyException(e));
        }
      }
    }

    @Override
    public void write(DataOutput out) throws IOException {
      if (!isPermissionLoaded()) {
        loadPermissionInfo();
      }
      super.write(out);
    }
  }

  /**
   * Use the command chown to set owner.
   */
  @Override
  public void setOwner(Path p, String username, String groupname)
    throws IOException {
    if (username == null && groupname == null) {
      throw new IOException("username == null && groupname == null");
    }

    if (username == null) {
      execCommand(pathToFile(p), Shell.SET_GROUP_COMMAND, groupname);
    } else {
      //OWNER[:[GROUP]]
      String s = username + (groupname == null? "": ":" + groupname);
      execCommand(pathToFile(p), Shell.SET_OWNER_COMMAND, s);
    }
  }

  /**
   * Use the command chmod to set permission.
   */
  @Override
  public void setPermission(Path p, FsPermission permission)
    throws IOException {
    execCommand(pathToFile(p), Shell.SET_PERMISSION_COMMAND,
        String.format("%05o", permission.toShort()));
  }

  private static String execCommand(File f, String... cmd) throws IOException {
    String[] args = new String[cmd.length + 1];
    System.arraycopy(cmd, 0, args, 0, cmd.length);
    args[cmd.length] = f.getCanonicalPath();
    String output = Shell.execCommand(args);
    return output;
  }

}
