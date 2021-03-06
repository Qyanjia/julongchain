/**
 * Copyright DingXuan. All Rights Reserved.
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
package org.bcia.julongchain.msp.mspconfig;

import org.bcia.julongchain.common.log.JulongChainLog;
import org.bcia.julongchain.common.log.JulongChainLogFactory;
import org.bcia.julongchain.common.util.CommConstant;
import org.bcia.julongchain.consenter.common.localconfig.ConsenterConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * mspconfig 文件加载
 *
 * @author zhangmingyang
 * @Date: 2018/3/29
 * @company Dingxuan
 */
public class MspConfigFactory {
    private static JulongChainLog log = JulongChainLogFactory.getLog(MspConfigFactory.class);

    private static MspConfig instance;

    public static MspConfig getMspConfig() {
        if (instance == null) {
            synchronized (MspConfigFactory.class) {
                if (instance == null) {
                    instance = loadMspConfig();
                }
            }
        }
        return instance;
    }

    public static MspConfig loadMspConfig() {
        Yaml yaml = new Yaml();

        InputStream is = null;
        MspConfig mspConfig=null;
        try {
            is = new FileInputStream(CommConstant.CONFIG_DIR_PREFIX + MspConfig.MSPCONFIG_FILE_PATH);
             mspConfig = yaml.loadAs(is, MspConfig.class);
            return mspConfig;
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return mspConfig;
    }
}
