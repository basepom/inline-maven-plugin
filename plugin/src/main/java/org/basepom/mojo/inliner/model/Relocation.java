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
package org.basepom.mojo.inliner.model;

import org.basepom.jarjar.transform.config.Rename;

public class Relocation {

    private String source;
    private String destination;
    private boolean hideClasses;
    \
    public Relocation() {
    }

    public String getSource() {
        return source;
    }

    public Relocation setSource(String source) {
        this.source = source;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public Relocation setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public boolean isHideClasses() {
        return hideClasses;
    }

    public Relocation setHideClasses(boolean hideClasses) {
        this.hideClasses = hideClasses;
        return this;
    }

    public Rename toRename() {
        return new Rename(this.source, this.destination, this.hideClasses);
    }
}
