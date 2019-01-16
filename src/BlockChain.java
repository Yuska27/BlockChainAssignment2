// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private TransactionPool txPool = new TransactionPool();
    private UTXOPool utxoPool = new UTXOPool();
    public HashMap<byte[] , BlockNode> nodesOfBlockChain = new HashMap<>();


    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        Date date = new Date();
        dateFormat.format(date);
        genesisBlock.finalize();
        BlockNode genesisNode = new BlockNode(genesisBlock, dateFormat.format(new Date()), 1);
        nodesOfBlockChain.put(genesisBlock.getHash(), genesisNode);
    }


    /** Get the maximum height block */
    public BlockNode getMaxHeightBlock() {
        BlockNode oneMaximumHeightNode =  nodesOfBlockChain.values().stream()
                                                                        .max(Comparator.comparing(node -> node.getHeight()))
                                                                        .orElse(null);

        final int maximum = (oneMaximumHeightNode != null) ? oneMaximumHeightNode.getHeight() : 1;

        List<BlockNode> maximumHeightNodes = nodesOfBlockChain.values().stream()
                                                                       .filter(node -> node.getHeight() == maximum)
                                                                       .collect(Collectors.toList());

        maximumHeightNodes.sort(Comparator.comparing(node -> node.getDateOfBlock()));
        return maximumHeightNodes.get(0); // oldest block of maxHeight is the first of this list
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return this.utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return this.txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // the genesis block is the only one that does not have a previous hash
        TxHandler txHandler = new TxHandler(this.utxoPool);
        byte[]  prevBlockHash = block.getPrevBlockHash();
        ArrayList<Transaction> blockTx = block.getTransactions();
        BlockNode prevBlock = this.nodesOfBlockChain.get(prevBlockHash);
        BlockNode maxHeightBlock = this.getMaxHeightBlock();
        
        if(block.getPrevBlockHash() == null)
            return false;
        
        if(prevBlock.getHeight() < maxHeightBlock.getHeight() - CUT_OFF_AGE)
            return false;


        Transaction[] validTxs = new Transaction[blockTx.size()];
        int i = 0;
        for(Transaction tx: blockTx){
            validTxs[i] = tx;
            i++;
        }
       txHandler.handleTxs(validTxs); // for some reason .toArray didn't work with casting

        // the genesis block is the only one that does not have a previous hash
        if(block.getPrevBlockHash() == null){
            return false;
        }
        else {
            TransactionPool newTxPool = new TransactionPool();
            for(Transaction tx : validTxs){
                newTxPool.addTransaction(tx);
            }
            UTXOPool newUTXOPool = new UTXOPool(txHandler.getUTXOPool());
            this.txPool = newTxPool;
            this.utxoPool = newUTXOPool;

            this.txPool.addTransaction(block.getCoinbase()); //the coinbase transaction is added if the block is valid
        }
        //Creating a new BlockNode and adding it to the hashmap in BlockChain
        nodesOfBlockChain.put(block.getRawBlock(), new BlockNode(block, dateFormat.format(new Date()), maxHeightBlock.getHeight()+1));

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }
}

