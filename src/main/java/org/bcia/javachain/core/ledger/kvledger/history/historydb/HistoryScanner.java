/*
 * Copyright Dingxuan. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.bcia.javachain.core.ledger.kvledger.history.historydb;

import org.bcia.javachain.common.exception.LedgerException;
import org.bcia.javachain.common.ledger.QueryResult;
import org.bcia.javachain.common.ledger.ResultsIterator;
import org.bcia.javachain.common.ledger.blkstorage.BlockStore;
import org.iq80.leveldb.DBIterator;

import java.util.Iterator;
import java.util.Map;

/**
 * 类描述
 *
 * @author sunzongyu
 * @date 2018/04/04
 * @company Dingxuan
 */
public class HistoryScanner implements ResultsIterator {
    private byte[] compositePartialKey = null;
    private String nameSpace = null;
    private String key = null;
    private DBIterator dbIter = null;
    private BlockStore blockStore = null;

    public HistoryScanner(byte[] compositePartialKey, String nameSpace
            , String key, DBIterator dbIter, BlockStore blockStore){
        this.compositePartialKey = compositePartialKey;
        this.nameSpace = nameSpace;
        this.key = key;
        this.dbIter = dbIter;
        this.blockStore = blockStore;
    }

    @Override
    public QueryResult next() throws LedgerException {
        if(!dbIter.hasNext()){
            return null;
        }
        Map.Entry<byte[], byte[]> entry = dbIter.next();

        return null;
    }

    @Override
    public void close() throws LedgerException {

    }
}