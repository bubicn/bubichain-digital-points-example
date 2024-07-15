package org.example;

import cn.bubi.SDK;
import cn.bubi.common.ToBaseUnit;
import cn.bubi.model.request.operation.BaseOperation;
import cn.bubi.model.response.AccountCreateResponse;
import cn.bubi.model.response.result.data.Signature;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

public class DigitalPointsDemo {
    // 初始化BubiChain SDK
    static String url = "https://seed1-node.bubi.cn";
    static SDK sdk = SDK.getInstance(url);

    // 生成积分发行方账户
    @Test
    public void createAccount() {
        AccountCreateResponse response = sdk.getAccountService().create();
        if (response.getErrorCode() != 0) {
            throw new RuntimeException(response.getErrorDesc());
        }
        System.out.println("createAccount response: " + response.getResult());
    }

    // 部署合约
    @Test
    public void deployContract() {
        // 账户部署合约所用的私钥
        String creatorPrivateKey = ExampleData.ACCOUNT_PRIVATE_KEY;

        // 部署BRC20 token的账户地址
        String creatorContractAddress = ExampleData.ACCOUNT_ADDRESS;
        // 合约初始化的Gas，单位UGas，1Gas = 10^8 UGas
        Long initBalance = ToBaseUnit.ToUGas("0.1");
        // token 名称
        String name = "Global";
        // token 代码
        String symbol = "GLA";
        // token 总供应量，包含小数位
        // 发行10亿个token，小数位为8，则需要1000000000 * 10^8
        String totalSupply = "1000000000";
        // 积分的小数位
        Integer decimals = 8;

        String version = "1.0";
        // 合约源代码
        String payload = ExampleData.CONTRACT_BRC20_CODE;

        // Init initInput
        JSONObject initInput = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("name", name);
        params.put("symbol", symbol);
        params.put("decimals", decimals);
        params.put("version", version);
        params.put("supply", totalSupply);
        initInput.put("params", params);

        String metadata = "deploy BRC20 contracts";
        // 获取合约发行方账户的nonce
        long nonce = BRC20ContractHelper.getAccountNonce(creatorContractAddress);
        BaseOperation[] operations = BRC20ContractHelper.buildCreateContractOperations(creatorContractAddress, payload, initInput.toJSONString(), initBalance, metadata);
        // 发行积分方账户地址
        String senderAddress = ExampleData.ACCOUNT_ADDRESS;
        // 设置最大花费10.1Gas，单位UGas
        Long feeLimit = ToBaseUnit.ToUGas("10.1");

        String transactionBlob = BRC20ContractHelper.buildTransactionBlob(senderAddress,nonce, feeLimit, operations);
        Signature[] signatures = BRC20ContractHelper.signTransaction(creatorPrivateKey, transactionBlob);
        System.out.println("signData: " + signatures[0].getSignData());
        System.out.println("publicKey: " + signatures[0].getPublicKey());
        String hash = BRC20ContractHelper.submitTransaction(transactionBlob, signatures);
        System.out.println("hash: " + hash);

        // 模拟延迟
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int status = BRC20ContractHelper.checkTransactionStatus(hash);
        System.out.println("status: " + status);

        // 获取合约地址， 需要做延迟处理，才可以获取到合约地址
        System.out.println("contractAddress: " + BRC20ContractHelper.getContractAddress(hash));
    }

    /**
     * 转移积分
     */
    @Test
    public void transfer() {
        // Init variable
        // The account private key to invoke contract
        String invokePrivateKey = ExampleData.ACCOUNT_PRIVATE_KEY;
        // The contract address , you can get it from create contract transaction
        String contractAddress = ExampleData.CONTRACT_ADDRESS;

        // The account to receive the BRC20 token
        String destAddress = ExampleData.RECEIVER_ADDRESS;

        // 0 means that the contract is only triggered
        long amount = 0L;
        // The fixed write 1000L, the unit is UGas
        Long gasPrice = 1000L;
        // Set up the maximum cost 0.01Gas
        Long feeLimit = ToBaseUnit.ToUGas("0.01");
        // The amount of BRC20 token to transfer, because the decimals is 8, so the unit is UGas
        long toAmount = ToBaseUnit.ToUGas(ExampleData.SEND_DIGITAL_POINTS_AMOUNT);
        // Contract main function entry

        JSONObject input = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("to", destAddress);
        params.put("value", toAmount+"");
        input.put("params", params);
        input.put("method", "transfer");

        String inputMethod = input.toJSONString();

        // 1. Get the account address to send this transaction
        String invokerAddress = ExampleData.ACCOUNT_ADDRESS;

        // Transaction initiation account's Nonce + 1
        Long nonce = BRC20ContractHelper.getAccountNonce(invokerAddress);

        // 2. Build sendAsset operation
        BaseOperation[] operations = BRC20ContractHelper.buildInvokeContractOperations(invokerAddress, contractAddress, amount, inputMethod);

        // 3. Build transaction blob

        String transactionBlob = BRC20ContractHelper.buildTransactionBlob(invokerAddress,nonce, feeLimit, operations);

        // 4. Sign the transaction

        Signature[] signatures = BRC20ContractHelper.signTransaction(invokePrivateKey, transactionBlob);

        // 5. Submit the transaction

        String txHash = BRC20ContractHelper.submitTransaction(transactionBlob, signatures);
        if(txHash != null) {
            System.out.println("hash: " + txHash);
        }
    }

    @Test
    public void getTransactionStatus() {
        String hash = ExampleData.SEND_DIGITAL_POINTS_TX_HASH;
        int status = BRC20ContractHelper.checkTransactionStatus(hash);
        if(status == 0) {
            System.out.println("Transaction status: " + status);
        } else {
            System.out.println("Transaction status: " + status);
        }
    }

    @Test
    public void callContract() {
        // The contract address , you can get it from create contract transaction
        String contractAddress = ExampleData.CONTRACT_ADDRESS;
        // The parameter to call

        JSONObject input = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("address", ExampleData.RECEIVER_ADDRESS);
        input.put("params", params);
        input.put("method", "balanceOf");
        String result = BRC20ContractHelper.callContract(contractAddress, input.toJSONString());
        JSONObject jsonObject = JSONObject.parseObject(result);
        if(jsonObject.get("query_rets") != null) {
            System.out.println(jsonObject.getJSONArray("query_rets").getJSONObject(0).getJSONObject("result").get("value"));
        }
        System.out.println();
    }
}
