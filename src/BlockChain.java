// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BlockChain {
    private static final int CUT_OFF_AGE = 10;

    private static final int NUMBER_OF_BLOCKS_IN_MEMORY = 1000;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private TransactionPool txPool = new TransactionPool();
    private UTXOPool utxoPool = new UTXOPool();

    // Look up table for blocks of the blockchain containing Hash and corresponding block as entries
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
    public Block getMaxHeightBlock() {
        //get one of the nodes with max height to extract the maximum height value from it
        BlockNode oneMaximumHeightNode =  nodesOfBlockChain.values().stream()
                                                                        .max(Comparator.comparing(node -> node.getHeight()))
                                                                        .orElse(null);

        final int maximum = (oneMaximumHeightNode != null) ? oneMaximumHeightNode.getHeight() : 1;
        //get all nodes with maximum height
        List<BlockNode> maximumHeightNodes = nodesOfBlockChain.values().stream()
                                                                       .filter(node -> node.getHeight() == maximum)
                                                                       .collect(Collectors.toList());

        //receive the oldest block of maximum height in case there are more than one element of maximum height
        if(maximumHeightNodes.size() > 1){
            maximumHeightNodes.sort(Comparator.comparing(node -> node.getDateOfBlock()));
        }

        return maximumHeightNodes.get(0).getBlockOfThisNode(); // oldest block of maxHeight is the first of this list
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        ArrayList<Transaction> maxHeightBlockTxs = getMaxHeightBlock().getTransactions();
        UTXOPool maxHeightUTXOPool = new UTXOPool();

        //adding all the outputs from transactions that are in the max heigth block
        for(Transaction tx : maxHeightBlockTxs) {
            for(int index = 0; index <tx.getOutputs().size(); index++) {
                maxHeightUTXOPool.addUTXO(new UTXO(tx.getHash(), index), tx.getOutput(index));
            }
        }
        return maxHeightUTXOPool;
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
        TxHandler txHandler = new TxHandler(this.utxoPool);
        byte[]  prevBlockHash = block.getPrevBlockHash();
        ArrayList<Transaction> blockTx = block.getTransactions();
        BlockNode prevBlock = this.nodesOfBlockChain.get(prevBlockHash);
        BlockNode maxHeightBlock = nodesOfBlockChain.get(getMaxHeightBlock().getHash());

        // the genesis block is the only one that does not have a previous hash
        if(block.getPrevBlockHash() == null){
            return false;
        }

        if(prevBlock.getHeight() < maxHeightBlock.getHeight() - CUT_OFF_AGE){
            return false;
        }

        Transaction[] validTxs = new Transaction[blockTx.size()];
        int i = 0;
        for(Transaction tx: blockTx){
            validTxs[i] = tx;
            i++;
        }
       txHandler.handleTxs(validTxs);

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
        nodesOfBlockChain.put(block.getHash(), new BlockNode(block, dateFormat.format(new Date()), nodesOfBlockChain.get(block.getPrevBlockHash()).getHeight()+1));

        //if 1000 nodes are in the current blockchain, the number is reduced to
        if(nodesOfBlockChain.size() >= NUMBER_OF_BLOCKS_IN_MEMORY){
            HashMap<byte[], BlockNode> newMap = new HashMap<>();
            //split the hashmap and take the newer half
            int half = nodesOfBlockChain.entrySet().size()/2;

            for(HashMap.Entry<byte[], BlockNode> entry : nodesOfBlockChain.entrySet()){
                if(entry.getValue().getHeight() >= half){
                    newMap.put(entry.getKey(), entry.getValue());
                }
                else{
                    //persist the other nodes in some external File
                }
            }
            nodesOfBlockChain = newMap; //only the 500 latest are inside the blockChain
        }
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    public String printBlockChain() {
        BlockNode headBlock = nodesOfBlockChain.get(getMaxHeightBlock().getHash());
        String activeBlockChain = new String(); 
        do {
            activeBlockChain += headBlock.getBlockOfThisNode().getHash();
            activeBlockChain += "-->";
            headBlock = this.nodesOfBlockChain.get(headBlock.getBlockOfThisNode().getPrevBlockHash());
        } while(headBlock.getBlockOfThisNode().getPrevBlockHash() != null);
        activeBlockChain += "genesis";
        return(activeBlockChain);
    }

    public class BlockNode{ //wrap a block with height and date
        //unidirected tree node, each block does not know it's successor
        private Block blockOfThisNode; //Block
        private String dateOfBlock; //relevant to select a the maxHeightBlock in case multiple blocks have the maxHeight
        private int height; // height of the block, which is increased incrementally

        public BlockNode( Block blockOfThisNode, String dateOfBlock, int height){
            this.blockOfThisNode = blockOfThisNode;
            this.dateOfBlock = dateOfBlock;
            this.height = height;
        }

        public Block getBlockOfThisNode(){
            return this.blockOfThisNode;
        }

        public String getDateOfBlock(){
            return this.dateOfBlock;
        }

        public int getHeight(){
            return height;
        }
    }
}

