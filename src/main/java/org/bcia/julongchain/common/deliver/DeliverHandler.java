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
package org.bcia.julongchain.common.deliver;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.bcia.julongchain.common.exception.ConsenterException;
import org.bcia.julongchain.common.exception.LedgerException;
import org.bcia.julongchain.common.exception.ValidateException;
import org.bcia.julongchain.common.ledger.blockledger.IIterator;
import org.bcia.julongchain.common.ledger.blockledger.file.FileLedgerIterator;
import org.bcia.julongchain.common.log.JavaChainLog;
import org.bcia.julongchain.common.log.JavaChainLogFactory;
import org.bcia.julongchain.common.util.Expiration;
import org.bcia.julongchain.consenter.common.multigroup.ChainSupport;
import org.bcia.julongchain.consenter.util.CommonUtils;
import org.bcia.julongchain.core.ledger.kvledger.txmgmt.statedb.QueryResult;
import org.bcia.julongchain.protos.common.Common;
import org.bcia.julongchain.protos.consenter.Ab;

import java.util.Map;

/**
 * @author zhangmingyang
 * @Date: 2018/5/29
 * @company Dingxuan
 */
public class DeliverHandler implements IHandler {
    private static JavaChainLog log = JavaChainLogFactory.getLog(DeliverHandler.class);
    private ISupportManager sm;
    private long timeWindow;
    private boolean mutualTLS;
    public DeliverHandler(ISupportManager sm, long timeWindow) {
        this.sm = sm;
        this.timeWindow = timeWindow;
    }

    public DeliverHandler(ISupportManager sm, long timeWindow, boolean mutualTLS) {
        this.sm = sm;
        this.timeWindow = timeWindow;
        this.mutualTLS = mutualTLS;
    }

    @Override
    public void handle(DeliverServer server) throws ValidateException, InvalidProtocolBufferException, LedgerException {
        try {
            delivrBlocks(server,server.getEnvelope());
        } catch (ConsenterException e) {
            e.printStackTrace();
        }
    }

    public void delivrBlocks(DeliverServer server, Common.Envelope envelope) throws ConsenterException, ValidateException, InvalidProtocolBufferException, LedgerException {
        Common.Payload payload = CommonUtils.unmarshalPayload(envelope.getPayload().toByteArray());

        sendStatusReply(server, Common.Status.BAD_REQUEST);

        if (payload.getHeader() == null) {
            log.warn(String.format("Malformed envelope received bad header"));
            sendStatusReply(server, Common.Status.BAD_REQUEST);
        }
        Common.GroupHeader chdr = CommonUtils.unmarshalGroupHeader(payload.getHeader().getGroupHeader().toByteArray());

        sendStatusReply(server, Common.Status.BAD_REQUEST);

        validateGroupHeader(server, chdr);
        sendStatusReply(server, Common.Status.BAD_REQUEST);

        ChainSupport chain = sm.getChain(chdr.getGroupId());
        if (chain == null) {
            log.debug(String.format("Rejecting deliver  channel %s not found", chdr.getGroupId()));
            sendStatusReply(server, Common.Status.NOT_FOUND);
        }

        //TODO select case  erroredChan

        SessionAc accessControl = new SessionAc(chain, server.getPolicyChecker(), chdr.getGroupId(), envelope, new Expiration());
        sendStatusReply(server, Common.Status.BAD_REQUEST);

        accessControl.enaluate();
        sendStatusReply(server, Common.Status.FORBIDDEN);
        Ab.SeekInfo seekInfo = null;
        try {
            seekInfo = Ab.SeekInfo.parseFrom(payload.getData().toByteArray());
        } catch (InvalidProtocolBufferException e) {
            sendStatusReply(server, Common.Status.BAD_REQUEST);
            throw new InvalidProtocolBufferException(e);
        }

        if (seekInfo.getStart() == null || seekInfo.getStop() == null) {
            sendStatusReply(server, Common.Status.BAD_REQUEST);
        }

       // IIterator cursor = chain.reader().iterator(seekInfo.getStart());
        IIterator cursor = chain.getLedgerResources().getReadWriteBase().iterator(seekInfo.getStart());
        if (cursor instanceof FileLedgerIterator) {
            FileLedgerIterator fileLedgerIterator = (FileLedgerIterator) cursor;
            long number = fileLedgerIterator.getBlockNum();
            long stopNumber;
            Ab.SeekPosition.TypeCase stop = seekInfo.getStop().getTypeCase();
            switch (stop) {
                case OLDEST:
                    stopNumber = number;
                case NEWEST:
                    stopNumber = chain.getLedgerResources().getReadWriteBase().height() - 1;
                case SPECIFIED:
                    stopNumber = stop.getNumber();
                    if (stopNumber < number) {
                        log.warn(String.format("[channel: %s] Received invalid seekInfo message from %s: start number %d greater than stop number %d", chdr.getGroupId(), number, stopNumber));
                        sendStatusReply(server, Common.Status.BAD_REQUEST);
                    }
            }

            cursor.close();
            for (; ; number++) {
                if (seekInfo.getBehavior()==Ab.SeekInfo.SeekBehavior.FAIL_IF_NOT_READY) {
                    if(number>chain.getLedgerResources().getReadWriteBase().height()-1){
                        sendStatusReply(server,Common.Status.NOT_FOUND);
                    }
                }



            }
        }


    }


    public Map.Entry<Common.Block,Common.Status> nextBlock(IIterator cursor) throws LedgerException, InvalidProtocolBufferException {
        // FIXME: 2018/5/31

        Map.Entry<QueryResult, Common.Status>  block = (Map.Entry<QueryResult, Common.Status>) cursor.next();

        ByteString byteString= (ByteString) block.getKey().getObj();
        Common.Block blockData=Common.Block.parseFrom(byteString);

        return null;

    }

    public void validateGroupHeader(DeliverServer server, Common.GroupHeader chdr) throws ConsenterException {
        if (chdr.getTimestamp() == null) {
            throw new ConsenterException("group header in envelope must contain timestamp");
        }

    }

    public void sendStatusReply(DeliverServer srv, Common.Status status) throws InvalidProtocolBufferException {
        srv.send(new DeliverHandlerSupport().createStatusReply(status));
    }

    public void sendBlockReply(DeliverServer srv, Common.Block block) throws InvalidProtocolBufferException {
        srv.send(new DeliverHandlerSupport().createBlockReply(block));
    }

    public ISupportManager getSm() {
        return sm;
    }

    public long getTimeWindow() {
        return timeWindow;
    }

}
