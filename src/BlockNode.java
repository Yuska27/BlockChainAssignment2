public class BlockNode{

    //unidirected tree node, each block does not know it's successor
    private Block blockOfThisNode;
    private String dateOfBlock;
    private int height;

    public BlockNode(Block blockOfThisNode, String dateOfBlock, int height){
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
