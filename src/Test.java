import java.security.*;
import java.util.Random;

public class Test {
    
    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException,
    SignatureException {
        
        // txHandler holds all randomly generated transactions
        TxHandler txHandler;
        
        Transaction tx;
        Random random = new Random();
        int numBitsKeyPair = 512;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(numBitsKeyPair);
        KeyPair scroogeKeyPair = keyPairGenerator.genKeyPair();
        KeyPair aliceKeyPair = keyPairGenerator.genKeyPair();
        KeyPair bobKeyPair = keyPairGenerator.genKeyPair();
        
        // Create initial UTXO-Pool with coins to be spend later
        UTXOPool utxoPool = new UTXOPool();
        int numInitialUTXOs = 20;
        int maxValueOutput = 20;
        
        for (int i = 0; i < numInitialUTXOs; i++){
            tx = new Transaction();
            int numOutput = random.nextInt(maxValueOutput) + 1;
            
            for (int j = 0; j < numOutput; j++){
                double value = random.nextDouble() + 1;
                tx.addOutput(value, scroogeKeyPair.getPublic());    // assign all coins to scrooge
            }
            tx.finalize();
            // add all outputs as Unspent Transaction Output
            for (int j = 0; j < numOutput; j++){
                UTXO utxo = new UTXO(tx.getHash(), j);
                utxoPool.addUTXO(utxo, tx.getOutput(j));
            }
        }
        // txHandler is now initialized
        txHandler = new TxHandler(new UTXOPool(utxoPool));
        
        // Example Test: test isValidTx() with one valid transaction from scrooge to alice
        Transaction validTx = new Transaction();
        // choose an unspent transaction as input
        UTXO utxo = utxoPool.getAllUTXO().get(0);
        validTx.addInput(utxo.getTxHash(), utxo.getIndex());
        // assign whole input to alice
        double inputValue = utxoPool.getTxOutput(utxo).value;
        double outputValue = inputValue;
        validTx.addOutput(outputValue, aliceKeyPair.getPublic());
        // uncommenting the next line tests for doublespending within one transaction
//        validTx.addOutput(outputValue, bobKeyPair.getPublic());
        
        // sign transaction using scrooge's private key
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(scroogeKeyPair.getPrivate());
        signature.update(validTx.getRawDataToSign(0));
        validTx.addSignature(signature.sign(), 0);
        
        validTx.finalize();
        
        // check for validity
        if(txHandler.isValidTx(validTx)){
            System.out.println("Transaction is valid.\n");
        }else{
            System.out.println("Transaction is invalid.\n");
        }
        
        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[]{validTx});
        if(acceptedTx.length == 1){
            System.out.println("Transaction is accepted.\n");
        }else{
            System.out.println("Transaction is not accepted.\n");
        }

        Block genesis = new Block(null, aliceKeyPair.getPublic());
        genesis.finalize();

        BlockChain blockChain = new BlockChain(genesis);
        BlockHandler blockHandler = new BlockHandler(blockChain);
        System.out.println("Genesis Block added.\n");


        Transaction validTransaction = new Transaction();
        validTransaction.addOutput(1,aliceKeyPair.getPublic());
        validTransaction.finalize();
        blockHandler.processTx(validTransaction);
        System.out.println("Transactions in the blockchain txPool: " + blockChain.getTransactionPool().getTransactions().size());

        Block block2 = new Block(genesis.getHash(),aliceKeyPair.getPublic());
        block2.finalize();
        blockHandler.processBlock(block2);
        System.out.println("New Block has been added via processBlock");
        System.out.println("Blockchain: " + blockChain.printBlockChain()+ "\n");

        Block block3 = new Block(block2.getHash(),bobKeyPair.getPublic());
        block3.finalize();
        blockHandler.processBlock(block3);
        System.out.println("New Block has been added via processBlock");
        System.out.println("Blockchain: " + blockChain.printBlockChain()+ "\n");

        Block block4 = new Block(block3.getHash(),bobKeyPair.getPublic());
        block4.finalize();
        blockHandler.processBlock(block4);
        System.out.println("New Block has been added via processBlock");
        System.out.println("Blockchain: " + blockChain.printBlockChain()+ "\n");

        Block block5 = new Block(block2.getHash(),bobKeyPair.getPublic());
        block5.finalize();
        blockHandler.processBlock(block5);
        System.out.println("Forked, Block5 --> Block 2 but the main Blockchain is still built on the one with maxHeight");


        //create Block also puts a new block into the blockchain successfully
        System.out.println("New Block has been added via createBlock");
        blockHandler.createBlock(aliceKeyPair.getPublic());
        System.out.println("Blockchain: " + blockChain.printBlockChain()+ "\n");

        System.out.println("New Block has been added via createBlock");
        blockHandler.createBlock(aliceKeyPair.getPublic());
        System.out.println("Blockchain: " + blockChain.printBlockChain()+ "\n");

        System.out.println("Max Height: " + blockChain.nodesOfBlockChain.get(blockChain.getMaxHeightBlock().getHash()).getHeight());
    }
}
