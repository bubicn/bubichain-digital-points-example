package org.example;

import cn.bubi.SDK;
import cn.bubi.model.request.*;
import cn.bubi.model.request.operation.BaseOperation;
import cn.bubi.model.request.operation.ContractCreateOperation;
import cn.bubi.model.request.operation.ContractInvokeByGasOperation;
import cn.bubi.model.response.*;
import cn.bubi.model.response.result.data.Signature;
import cn.bubi.model.response.result.data.TransactionHistory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class BRC20ContractHelper {
    public static SDK sdk;
    public static String url = "https://seed1-node.bubi.cn";

    // 加载类时初始化 sdk
    static {
        sdk = SDK.getInstance(url);
    }

    public static long getAccountNonce(String accountAddress) {
        long nonce = 0;

        // Init request
        AccountGetNonceRequest request = new AccountGetNonceRequest();
        request.setAddress(accountAddress);

        // Call getNonce
        AccountGetNonceResponse response = sdk.getAccountService().getNonce(request);
        if (0 == response.getErrorCode()) {
            nonce = response.getResult().getNonce();
        } else {
            System.out.println("error: " + response.getErrorDesc());
        }
        return nonce;
    }
    public static BaseOperation[] buildCreateContractOperations(String createContractAddress, String payload, String initInput, Long initBalance, String metadata) {
        // Build create contract operation
        ContractCreateOperation contractCreateOperation = new ContractCreateOperation();
        contractCreateOperation.setSourceAddress(createContractAddress);
        contractCreateOperation.setInitBalance(initBalance);
        contractCreateOperation.setPayload(payload);
        contractCreateOperation.setInitInput(initInput);
        contractCreateOperation.setMetadata(metadata);

        return new BaseOperation[]{ contractCreateOperation };
    }

    public static BaseOperation[] buildInvokeContractOperations(String invokeAddress, String contractAddress, long amount, String input) {
        // Build create contract operation
        ContractInvokeByGasOperation operation = new ContractInvokeByGasOperation();
        operation.setSourceAddress(invokeAddress);
        operation.setContractAddress(contractAddress);
        operation.setBuAmount(amount);
        operation.setInput(input);

        return new BaseOperation[]{ operation };
    }

    public static String buildTransactionBlob(String senderAddress, Long nonce, Long feeLimit,  BaseOperation[] operations) {
        String transactionBlob = null;
        // The gasPrice is fixed at 1000L, the unit is UGas
        Long gasPrice = 1000L;
        // Nonce should add 1
        nonce += 1;

        // Build transaction  Blob
        TransactionBuildBlobRequest transactionBuildBlobRequest = new TransactionBuildBlobRequest();
        transactionBuildBlobRequest.setSourceAddress(senderAddress);
        transactionBuildBlobRequest.setNonce(nonce);
        transactionBuildBlobRequest.setFeeLimit(feeLimit);
        transactionBuildBlobRequest.setGasPrice(gasPrice);
        for (BaseOperation operation : operations) {
            transactionBuildBlobRequest.addOperation(operation);
        }
        TransactionBuildBlobResponse transactionBuildBlobResponse = sdk.getTransactionService().buildBlob(transactionBuildBlobRequest);
        if (transactionBuildBlobResponse.getErrorCode() == 0) {
            transactionBlob = transactionBuildBlobResponse. getResult().getTransactionBlob();
        } else {
            System.out.println("error: " + transactionBuildBlobResponse.getErrorDesc());
        }
        return transactionBlob;
    }

    public static Signature[] signTransaction(String signerPrivateKey, String transactionBlob) {
        Signature[] signatures = null;

        // Sign transaction BLob
        TransactionSignRequest transactionSignRequest = new TransactionSignRequest();
        transactionSignRequest.setBlob(transactionBlob);
        transactionSignRequest.addPrivateKey(signerPrivateKey);
        TransactionSignResponse transactionSignResponse = sdk.getTransactionService().sign(transactionSignRequest);
        if (transactionSignResponse.getErrorCode() == 0) {
            signatures = transactionSignResponse.getResult().getSignatures();
        } else {
            System.out.println("error: " + transactionSignResponse.getErrorDesc());
        }
        return signatures;
    }

    public static String submitTransaction(String transactionBlob, Signature[] signatures) {
        String  hash = null;

        // Submit transaction
        TransactionSubmitRequest transactionSubmitRequest = new TransactionSubmitRequest();
        transactionSubmitRequest.setTransactionBlob(transactionBlob);
        transactionSubmitRequest.setSignatures(signatures);
        TransactionSubmitResponse transactionSubmitResponse = sdk.getTransactionService().submit(transactionSubmitRequest);
        if (0 == transactionSubmitResponse.getErrorCode()) {
            hash = transactionSubmitResponse.getResult().getHash();
        } else {
            System.out.println("error: " + transactionSubmitResponse.getErrorDesc());
        }
        return  hash ;
    }


    public static int checkTransactionStatus(String txHash) {
        // Init request
        TransactionGetInfoRequest request = new TransactionGetInfoRequest();
        request.setHash(txHash);

        // Call getInfo
        TransactionGetInfoResponse response = sdk.getTransactionService().getInfo(request);
        int errorCode = response.getErrorCode();
        if (errorCode == 0){
            TransactionHistory transactionHistory = response.getResult().getTransactions()[0];
            errorCode = transactionHistory.getErrorCode();
        }

        return errorCode;
    }

    public static String getContractAddress(String hash) {
        ContractGetAddressRequest request = new ContractGetAddressRequest();
        request.setHash(hash);

        // Call getAddress
        ContractGetAddressResponse response = sdk.getContractService().getAddress(request);
        if (response.getErrorCode() == 0) {
            return (JSON.toJSONString(response.getResult(), true));
        } else {
            System.out.println("error: " + response.getErrorDesc());
        }

        return null;
    }

    public static String callContract(String contractAddress, String input) {
        ContractCallRequest request = new ContractCallRequest();
        request.setContractAddress(contractAddress);
        // The gasLimit is fixed at 0L, the unit is UGas,Because querying type contract methods generally does not trigger on-chain transactions
        request.setFeeLimit(0L);
        request.setOptType(ContractCallOptType.QUERY.getValue());
        request.setInput(input);
        ContractCallResponse response = sdk.getContractService().call(request);
        if (response.getErrorCode() == 0) {
            return (JSON.toJSONString(response.getResult(), true));
        } else {
            System.out.println("error: " + response.getErrorDesc());
        }
        return null;
    }
}
