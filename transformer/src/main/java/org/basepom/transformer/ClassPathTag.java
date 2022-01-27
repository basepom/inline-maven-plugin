/*
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
package org.basepom.transformer;

public enum ClassPathTag {
    FILE,          // tags a file
    DIRECTORY,     // tags a directory
    CLASS,         // is a class
    RESOURCE,      // is a resource

    ROOT_JAR       // one or more root jars, should not contain relocations, files in META-INF/ are retained
}