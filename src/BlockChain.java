// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    public static boolean genesis = false; //the first block a.k.a. genesisBlock sets this to true only once per blockchain
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private HashMap<byte[] , BackwardsNode> nodesOfBlockChain = new HashMap<>();
    private BlockHandler blockHandler = new BlockHandler(this);

    private TransactionPool txPool = new TransactionPool();
    private UTXOPool utxoPool = new UTXOPool();
    // private PublicKey myAddress;

    // public BlockChain(PublicKey myAddress){
    //     this.myAddress = myAddress;
    // }

    private class BlockNode{

        //unidirected tree node, each block does not know it's successor
        private byte[] previousNodeHash;
        private Block blockOfThisNode;
        private Date  dateOfBlock;
        private int height;

        public BlockNode(byte[] previousNodeHash, Block blockOfThisNode, Date dateOfBlock, int height){
            this.previousNodeHash = previousNodeHash;
            this.blockOfThisNode = blockOfThisNode;
            this.dateOfBlock = dateOfBlock;
            this.height = height;

            // if(!genesis){
            //     genesis = true;
            // }

            // if(previousNode == null && genesis){
            //     throw new IllegalArgumentException("The genesis Block already exists!");
            // }
        }
        public getBlockHash() {
            return this.blockOfThisNode.getHash();
        }
    }

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        Date date = new Date();
        dateFormat.format(date);

        BlockNode genesisNode = new BlockNode(null, genesisBlock, date, 0);
        nodesOfBlockChain.put(genesisBlock.getHash(), genesisNode);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {

        BlockNode oneMaximumHeightNode =  nodesOfBlockChain.values().stream()
                                                                        .max(Comparator.comparing(node -> node.height))
                                                                        .orElse(null);

        final int maximum = (oneMaximumHeightNode != null) ? oneMaximumHeightNode.height : 1;

        List<BlockNode> maximumHeightNodes = nodesOfBlockChain.values().stream()
                                                                        .filter(node -> node.height == maximum)
                                                                        .collect(Collectors.toList());

        maximumHeightNodes.sort(Comparator.comparing(node -> node.dateOfBlock));
        return maximumHeightNodes.get(0).blockOfThisNode; // oldest block of maxHeight is the first of this list
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
        ArrayList<Transaction> blockTx= block.getTransactions();
        BlockNode prevBlock = this.nodesOfBlockChain.get(prevBlockHash);
        BlockNode maxHeightBlock = this.getMaxHeightBlock();
        
        if(block.getPrevBlockHash() == null)
            return false;
        
        if(prevBlock.height < maxHeightBlock.height - CUT_OFF_AGE)
            return false;
        
        Transaction[] validTxs = txHandler.handleTxs(blockTx.toArray());

        if(validTxs.length != blockTx.toArray().length)
            return false;

        else {
            TransactionPool newTxPool = new TransactionPool();
            for(Transaction tx : validTxs){
                newTxPool.add(tx);
            }
            UTXOPool newUTXOPool = new UTXOPool(txHandler.getUTXOPool());
            this.txPool = newTxPool;
            this.utxoPool = newUTXOPool;
        }

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        this.txPool.addTransaction(tx);
    }
}