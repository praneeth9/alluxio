/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.shell.command;

import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.client.ClientContext;
import alluxio.client.FileSystemTestUtils;
import alluxio.client.WriteType;
import alluxio.client.util.ClientTestUtils;
import alluxio.exception.ExceptionMessage;
import alluxio.shell.AbstractAlluxioShellTest;
import alluxio.shell.AlluxioShellUtilsTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for persist command.
 */
public class PersistCommandTest extends AbstractAlluxioShellTest {
  @Test
  public void persistTest() throws Exception {
    String testFilePath = "/testPersist/testFile";
    FileSystemTestUtils.createByteFile(mFileSystem, testFilePath, WriteType.MUST_CACHE, 10);
    Assert
        .assertFalse(mFileSystem.getStatus(new AlluxioURI("/testPersist/testFile")).isPersisted());

    int ret = mFsShell.run("persist", testFilePath);
    Assert.assertEquals(0, ret);
    Assert.assertEquals("persisted file " + testFilePath + " with size 10\n", mOutput.toString());
    checkFilePersisted(new AlluxioURI("/testPersist/testFile"), 10);
  }

  @Test
  public void persistDirectoryTest() throws Exception {
    // Set the default write type to MUST_CACHE, so that directories are not persisted by default
    ClientContext.getConf().set(Constants.USER_FILE_WRITE_TYPE_DEFAULT, "MUST_CACHE");
    AlluxioShellUtilsTest.resetFileHierarchy(mFileSystem);
    Assert.assertFalse(mFileSystem.getStatus(new AlluxioURI("/testWildCards")).isPersisted());
    Assert
        .assertFalse(mFileSystem.getStatus(new AlluxioURI("/testWildCards/foo")).isPersisted());
    Assert
        .assertFalse(mFileSystem.getStatus(new AlluxioURI("/testWildCards/bar")).isPersisted());
    int ret = mFsShell.run("persist", "/testWildCards");
    Assert.assertEquals(0, ret);
    Assert.assertTrue(mFileSystem.getStatus(new AlluxioURI("/testWildCards")).isPersisted());
    Assert
        .assertTrue(mFileSystem.getStatus(new AlluxioURI("/testWildCards/foo")).isPersisted());
    Assert
        .assertTrue(mFileSystem.getStatus(new AlluxioURI("/testWildCards/bar")).isPersisted());
    checkFilePersisted(new AlluxioURI("/testWildCards/foo/foobar1"), 10);
    checkFilePersisted(new AlluxioURI("/testWildCards/foo/foobar2"), 20);
    checkFilePersisted(new AlluxioURI("/testWildCards/bar/foobar3"), 30);
    checkFilePersisted(new AlluxioURI("/testWildCards/foobar4"), 40);
    ClientTestUtils.resetClientContext();
  }

  @Test
  public void persistMultiFilesTest() throws Exception {
    String filePath1 = "/testPersist/testFile1";
    String filePath2 = "/testFile2";
    String filePath3 = "/testPersist/testFile3";
    FileSystemTestUtils.createByteFile(mFileSystem, filePath1, WriteType.MUST_CACHE, 10);
    FileSystemTestUtils.createByteFile(mFileSystem, filePath2, WriteType.MUST_CACHE, 20);
    FileSystemTestUtils.createByteFile(mFileSystem, filePath3, WriteType.MUST_CACHE, 30);

    Assert.assertFalse(mFileSystem.getStatus(new AlluxioURI(filePath1)).isPersisted());
    Assert.assertFalse(mFileSystem.getStatus(new AlluxioURI(filePath2)).isPersisted());
    Assert.assertFalse(mFileSystem.getStatus(new AlluxioURI(filePath3)).isPersisted());

    int ret = mFsShell.run("persist", filePath1, filePath2, filePath3);
    Assert.assertEquals(0, ret);
    checkFilePersisted(new AlluxioURI(filePath1), 10);
    checkFilePersisted(new AlluxioURI(filePath2), 20);
    checkFilePersisted(new AlluxioURI(filePath3), 30);
  }

  /**
   * Tests persisting files and directories together in one persist command.
   */
  @Test
  public void persistMultiFilesAndDirsTest() throws Exception {
    ClientContext.getConf().set(Constants.USER_FILE_WRITE_TYPE_DEFAULT, "MUST_CACHE");
    AlluxioShellUtilsTest.resetFileHierarchy(mFileSystem);
    Assert.assertFalse(mFileSystem.getStatus(new AlluxioURI("/testWildCards")).isPersisted());
    Assert.assertFalse(mFileSystem.getStatus(new AlluxioURI("/testWildCards/foo")).isPersisted());
    Assert.assertFalse(
        mFileSystem.getStatus(new AlluxioURI("/testWildCards/foo/foobar2")).isPersisted());
    Assert.assertFalse(mFileSystem.getStatus(new AlluxioURI("/testWildCards/bar")).isPersisted());

    int ret = mFsShell.run("persist", "/testWildCards/foo/foobar1", "/testWildCards/foobar4",
        "/testWildCards/bar", "/testWildCards/bar/foobar3");
    Assert.assertEquals(0, ret);
    Assert.assertTrue(mFileSystem.getStatus(new AlluxioURI("/testWildCards")).isPersisted());
    Assert.assertTrue(mFileSystem.getStatus(new AlluxioURI("/testWildCards/foo")).isPersisted());
    Assert.assertFalse(
        mFileSystem.getStatus(new AlluxioURI("/testWildCards/foo/foobar2")).isPersisted());
    Assert.assertTrue(mFileSystem.getStatus(new AlluxioURI("/testWildCards/bar")).isPersisted());
    checkFilePersisted(new AlluxioURI("/testWildCards/foo/foobar1"), 10);
    checkFilePersisted(new AlluxioURI("/testWildCards/bar/foobar3"), 30);
    checkFilePersisted(new AlluxioURI("/testWildCards/foobar4"), 40);
    ClientTestUtils.resetClientContext();
  }

  @Test
  public void persistNonexistentFileTest() throws Exception {
    // Cannot persist a nonexistent file
    String path = "/testPersistNonexistent";
    int ret = mFsShell.run("persist", path);
    Assert.assertEquals(-1, ret);
    Assert.assertEquals(ExceptionMessage.PATH_DOES_NOT_EXIST.getMessage(path) + "\n",
        mOutput.toString());
  }

  @Test
  public void persistTwiceTest() throws Exception {
    // Persisting an already-persisted file is okay
    String testFilePath = "/testPersist/testFile";
    FileSystemTestUtils.createByteFile(mFileSystem, testFilePath, WriteType.MUST_CACHE, 10);
    Assert
        .assertFalse(mFileSystem.getStatus(new AlluxioURI("/testPersist/testFile")).isPersisted());
    int ret = mFsShell.run("persist", testFilePath);
    Assert.assertEquals(0, ret);
    ret = mFsShell.run("persist", testFilePath);
    Assert.assertEquals(0, ret);
    Assert.assertEquals("persisted file " + testFilePath + " with size 10\n" + testFilePath
        + " is already persisted\n", mOutput.toString());
    checkFilePersisted(new AlluxioURI("/testPersist/testFile"), 10);
  }
}
