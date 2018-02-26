/*
 * #!
 * Ontopia Engine
 * #-
 * Copyright (C) 2001 - 2013 The Ontopia Project
 * #-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * !#
 */
package net.ontopia.topicmaps.utils.ltm;

import java.io.IOException;
import java.util.List;
import net.ontopia.utils.TestFileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LTMErrorTestCase {

  private final static String testdataDirectory = "ltm";

  private String filename;

  @Parameters
  public static List generateTests() {
    return TestFileUtils.getTestInputFiles(testdataDirectory, "error", ".ltm");
  }

  public LTMErrorTestCase(String root, String filename) {
    this.filename = filename;
  }

  @Test
  public void testFile() throws IOException {
    // produce canonical output
    String in = TestFileUtils.getTestInputFile(testdataDirectory, "error",
            filename);

    try {
      new LTMTopicMapReader(TestFileUtils.getTestInputURL(in)).read();
      Assert.fail("test file " + filename + " parsed without error");
    } catch (java.io.IOException e) {
    } catch (net.ontopia.topicmaps.core.UniquenessViolationException e) {
    } catch (net.ontopia.utils.OntopiaRuntimeException e) {
    }
  }
}
