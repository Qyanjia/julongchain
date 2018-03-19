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
package org.bcia.javachain.core.common.validation;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.lang3.ArrayUtils;
import org.bcia.javachain.common.exception.JavaChainException;
import org.bcia.javachain.common.exception.ValidateException;
import org.bcia.javachain.common.log.JavaChainLog;
import org.bcia.javachain.common.log.JavaChainLogFactory;
import org.bcia.javachain.common.util.proto.ProposalUtils;
import org.bcia.javachain.msp.IIdentity;
import org.bcia.javachain.msp.IIdentityDeserializer;
import org.bcia.javachain.node.common.helper.MockMSPManager;
import org.bcia.javachain.protos.common.Common;
import org.bcia.javachain.protos.node.ProposalPackage;

/**
 * 校验消息
 *
 * @author zhouhui
 * @date 2018/3/14
 * @company Dingxuan
 */
public class MsgValidation {
    private static JavaChainLog log = JavaChainLogFactory.getLog(MsgValidation.class);

    /**
     * 校验群组头部
     *
     * @param groupHeader
     * @return 返回扩展域是为了性能上的考虑，不用再次去读取或转化扩展域
     * @throws ValidateException
     */
    public static ProposalPackage.SmartContractHeaderExtension validateGroupHeader(Common.GroupHeader groupHeader) throws ValidateException {
        if (groupHeader == null) {
            throw new ValidateException("Missing groupHeader");
        }

        //校验消息类型
        if (groupHeader.getType() != Common.HeaderType.ENDORSER_TRANSACTION_VALUE
                && groupHeader.getType() != Common.HeaderType.CONFIG_UPDATE_VALUE
                && groupHeader.getType() != Common.HeaderType.CONFIG_VALUE
                && groupHeader.getType() != Common.HeaderType.NODE_RESOURCE_UPDATE_VALUE) {
            throw new ValidateException("Wrong message type: " + groupHeader.getType());
        }

        //校验纪元，此时应该是0
        if (groupHeader.getEpoch() != 0L) {
            throw new ValidateException("Wrong epoch: " + groupHeader.getEpoch());
        }

        //校验扩展域
        if (groupHeader.getType() == Common.HeaderType.ENDORSER_TRANSACTION_VALUE
                || groupHeader.getType() != Common.HeaderType.CONFIG_VALUE) {
            ProposalPackage.SmartContractHeaderExtension extension = null;
            try {
                extension = ProposalPackage.SmartContractHeaderExtension.parseFrom(groupHeader.getExtension());
            } catch (InvalidProtocolBufferException e) {
                log.error(e.getMessage(), e);
                //不能成功转化，说明是错误的智能合约头部扩展
                throw new ValidateException("Wrong SmartContractHeaderExtension");
            }

            if (extension.getSmartContractId() == null) {
                //智能合约标识为空
                throw new ValidateException("Missing SmartContractHeaderExtension SmartContractId");
            }

            return extension;

            //TODO:PayloadVisibility要判断吗
        }

        return null;
    }

    /**
     * 校验签名头部
     *
     * @param signatureHeader
     * @throws ValidateException
     */
    public static void validateSignatureHeader(Common.SignatureHeader signatureHeader) throws ValidateException {
        if (signatureHeader == null) {
            throw new ValidateException("Missing signatureHeader");
        }

        //校验随机数，应存在且有效
        if (signatureHeader.getNonce() == null || signatureHeader.getNonce().isEmpty()) {
            throw new ValidateException("Missing nonce");
        }

        //校验消息创建者，应存在且有效
        if (signatureHeader.getCreator() == null || signatureHeader.getCreator().isEmpty()) {
            throw new ValidateException("Missing creator");
        }
    }

    /**
     * 验证头部
     *
     * @param header
     * @return
     * @throws ValidateException
     */
    public static Object[] validateCommonHeader(Common.Header header) throws ValidateException {
        if (header == null) {
            throw new ValidateException("Missing header");
        }

        if (header.getGroupHeader() == null) {
            throw new ValidateException("Missing groupHeader");
        }

        Common.GroupHeader groupHeader = null;
        try {
            groupHeader = Common.GroupHeader.parseFrom(header.getGroupHeader());
        } catch (InvalidProtocolBufferException e) {
            log.error(e.getMessage(), e);
            //不能成功转化，说明是错误的群组头部
            throw new ValidateException("Wrong groupHeader");
        }

        //校验群组头部
        ProposalPackage.SmartContractHeaderExtension extension = validateGroupHeader(groupHeader);

        Common.SignatureHeader signatureHeader = null;
        try {
            signatureHeader = Common.SignatureHeader.parseFrom(header.getSignatureHeader());
        } catch (InvalidProtocolBufferException e) {
            log.error(e.getMessage(), e);
            //不能成功转化，说明是错误的签名头部
            throw new ValidateException("Wrong signatureHeader");
        }

        //校验签名头部
        validateSignatureHeader(signatureHeader);

        return new Object[]{groupHeader, signatureHeader, extension};
    }

    /**
     * 检查签名是否正确
     *
     * @param signature
     * @param message
     * @param creator
     * @param groupId
     * @throws ValidateException
     */
    public static void checkSignature(byte[] signature, byte[] message, byte[] creator, String groupId) throws ValidateException {
        if (ArrayUtils.isEmpty(signature) || ArrayUtils.isEmpty(message) || ArrayUtils.isEmpty(creator)) {
            throw new ValidateException("Missing arguments");
        }

        //获取反序列化器
        IIdentityDeserializer identityDeserializer = MockMSPManager.getIdentityDeserializer(groupId);

        //反序列化出身份对象
        IIdentity identity = identityDeserializer.deserializeIdentity(creator);
        //校验自身
        identity.validate();
        //校验签名
        identity.verify(message, signature);
    }

    /**
     * 检查提案中的交易id
     *
     * @param txId
     * @param creator
     * @param nonce
     * @throws ValidateException
     */
    public static void checkProposalTxID(String txId, byte[] creator, byte[] nonce) throws ValidateException {
        String expectTxID = null;
        try {
            expectTxID = ProposalUtils.computeProposalTxID(creator, nonce);
        } catch (JavaChainException e) {
            log.error(e.getMessage(), e);
            throw new ValidateException("Can not validate txId");
        }

        if (!expectTxID.equals(txId)) {
            throw new ValidateException("Wrong txId");
        }
    }
}
