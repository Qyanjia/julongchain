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
package org.bcia.javachain.common.util.proto;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.ArrayUtils;
import org.bcia.javachain.common.exception.JavaChainException;
import org.bcia.javachain.csp.gm.GmCspFactory;
import org.bcia.javachain.csp.intfs.ICsp;
import org.bcia.javachain.msp.ISigningIdentity;
import org.bcia.javachain.protos.common.Common;
import org.bcia.javachain.protos.node.ProposalPackage;
import org.bcia.javachain.protos.node.Smartcontract;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Proposal工具类
 *
 * @author zhouhui
 * @date 2018/3/12
 * @company Dingxuan
 */
public class ProposalUtils {
    /**
     * 构造带签名的提案
     *
     * @param proposal
     * @param identity
     * @return
     */
    public static ProposalPackage.SignedProposal buildSignedProposal(ProposalPackage.Proposal proposal,
                                                                     ISigningIdentity identity) {
        //获取SignedProposal构造器
        ProposalPackage.SignedProposal.Builder signedProposalBuilder = ProposalPackage.SignedProposal.newBuilder();
        signedProposalBuilder.setProposalBytes(proposal.toByteString());

        //计算签名字段
        byte[] signatureBytes = identity.sign(proposal.toByteArray());
        signedProposalBuilder.setSignature(ByteString.copyFrom(signatureBytes));

        return signedProposalBuilder.build();
    }

    /**
     * 构造Proposal对象
     *
     * @param txId
     * @param type
     * @param groupId
     * @param scis
     * @param nonce
     * @param creator
     * @param transientMap
     * @return
     */
    public static ProposalPackage.Proposal buildSmartContractProposal(Common.HeaderType type, String groupId, String
            txId, Smartcontract.SmartContractInvocationSpec scis, byte[] nonce, byte[] creator, Map<String, byte[]>
                                                                              transientMap) {
        //首先构造SmartContractHeaderExtension对象
        ProposalPackage.SmartContractHeaderExtension.Builder headerExtensionBuilder = ProposalPackage
                .SmartContractHeaderExtension.newBuilder();
        headerExtensionBuilder.setSmartContractId(scis.getSmartContractSpec().getSmartContractId());
        ProposalPackage.SmartContractHeaderExtension headerExtension = headerExtensionBuilder.build();

        //构造Header对象
        Common.Header header = EnvelopeHelper.buildHeader(type.getNumber(), 0, groupId, txId, 0,
                headerExtension, creator, nonce);

        //构造SmartContractProposalPayload对象
        ProposalPackage.SmartContractProposalPayload proposalPayload = buildProposalPayload(scis, transientMap);

        //构造Proposal对象
        ProposalPackage.Proposal.Builder proposalBuilder = ProposalPackage.Proposal.newBuilder();
        proposalBuilder.setHeader(header.toByteString());
        proposalBuilder.setPayload(proposalPayload.toByteString());

        return proposalBuilder.build();
    }

    /**
     * 构造Proposal的Payload字段
     *
     * @param scis
     * @param transientMap
     * @return
     */
    public static ProposalPackage.SmartContractProposalPayload buildProposalPayload(Smartcontract.SmartContractInvocationSpec scis, Map<String,
            byte[]> transientMap) {
        //SmartContractProposalPayload构造器
        ProposalPackage.SmartContractProposalPayload.Builder proposalPayloadBuilder = ProposalPackage
                .SmartContractProposalPayload.newBuilder();

        proposalPayloadBuilder.setInput(scis.toByteString());

        if (transientMap != null && !transientMap.isEmpty()) {
            for (String key : transientMap.keySet()) {
                proposalPayloadBuilder.putTransientMap(key, ByteString.copyFrom(transientMap.get(key)));
            }
        }

        return proposalPayloadBuilder.build();
    }

    /**
     * 生成交易ID
     *
     * @param creator
     * @param nonce
     * @return
     * @throws JavaChainException
     */
    public static String computeProposalTxID(byte[] creator, byte[] nonce) throws JavaChainException {
        long beginTime = System.currentTimeMillis();
        ICsp csp = new GmCspFactory().getCsp(null);

        long time1 = System.currentTimeMillis();
        System.out.println("1耗时" + (time1 - beginTime) + "ms");

        byte[] bytes1 = ArrayUtils.addAll(creator, nonce);

        long time2 = System.currentTimeMillis();
        System.out.println("2耗时" + (time2 - time1) + "ms");

        byte[] resultBytes = csp.hash(bytes1, null);

        long time3 = System.currentTimeMillis();
        System.out.println("3耗时" + (time3 - time2) + "ms");

        String txId = new String(resultBytes, Charset.forName("UTF-8"));

        long time4 = System.currentTimeMillis();
        System.out.println("4耗时" + (time4 - time3) + "ms");

        return txId;
    }
}
