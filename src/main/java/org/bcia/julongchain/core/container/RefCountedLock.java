/**
 * Copyright Dingxuan. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bcia.julongchain.core.container;

import org.bcia.julongchain.core.RWMutex;

/**
 * 类描述
 *
 * @author wanliangbing
 * @date 2018/4/2
 * @company Dingxuan
 */
public class RefCountedLock {

    public Integer getRefCount() {
        return refCount;
    }

    public void setRefCount(Integer refCount) {
        this.refCount = refCount;
    }

    public RWMutex getLock() {
        return lock;
    }

    public void setLock(RWMutex lock) {
        this.lock = lock;
    }

    private Integer refCount;
    private RWMutex lock;

}
