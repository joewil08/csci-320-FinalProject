package filesystem;

import java.io.IOException;


public class FileSystem {
    private Disk diskDevice;

    private int iNodeNumber;
    private int fileDescriptor;
    private INode iNodeForFile;
    private FreeBlockList freeBlockList;

    public FileSystem() throws IOException {
        diskDevice = new Disk();
        diskDevice.format();
        freeBlockList = new FreeBlockList();
    }

    /***
     * Create a file with the name <code>fileName</code>
     *
     * @param fileName - name of the file to create
     * @throws IOException
     */
    public int create(String fileName) throws IOException {
        INode tmpINode = null;

        boolean isCreated = false;

        for (int i = 0; i < Disk.NUM_INODES && !isCreated; i++) {
            tmpINode = diskDevice.readInode(i);
            String name = tmpINode.getFileName();
            if (tmpINode.getFileName() == null) {
                this.iNodeForFile = new INode();
                this.iNodeForFile.setFileName(fileName);
                this.iNodeNumber = i;
                this.fileDescriptor = i;
                isCreated = true;
            } else if (name.trim().equals(fileName)){
                throw new IOException("FileSystem::create: "+fileName+
                        " already exists");
            }
        }
        if (!isCreated) {
            throw new IOException("FileSystem::create: Unable to create file");
        }

        return fileDescriptor;
    }

    /**
     * Removes the file
     *
     * @param fileName
     * @throws IOException
     */
    public void delete(String fileName) throws IOException {
        INode tmpINode = null;
        boolean isFound = false;
        int inodeNumForDeletion = -1;

        /**
         * Find the non-null named inode that matches,
         * If you find it, set its file name to null
         * to indicate it is unused
         */
        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);

            String fName = tmpINode.getFileName();

            if (fName != null && fName.trim().compareTo(fileName.trim()) == 0) {
                isFound = true;
                inodeNumForDeletion = i;
                break;
            }
        }

        /***
         * If file found, go ahead and deallocate its
         * blocks and null out the filename.
         */
        if (isFound) {
            deallocateBlocksForFile(inodeNumForDeletion);
            tmpINode.setFileName(null);
            diskDevice.writeInode(tmpINode, inodeNumForDeletion);
            this.iNodeForFile = null;
            this.fileDescriptor = -1;
            this.iNodeNumber = -1;
        }
    }


    /***
     * Makes the file available for reading/writing
     *
     * @return
     * @throws IOException
     */
    public int open(String fileName) throws IOException {
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
        INode tmpINode = null;
        boolean isFound = false;
        int iNodeContainingName = -1;

        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);
            String fName = tmpINode.getFileName();
            if (fName != null) {
                if (fName.trim().compareTo(fileName.trim()) == 0) {
                    isFound = true;
                    iNodeContainingName = i;
                    this.iNodeForFile = tmpINode;
                }
            }
        }

        if (isFound) {
            this.fileDescriptor = iNodeContainingName;
            this.iNodeNumber = fileDescriptor;
        }

        return this.fileDescriptor;
    }


    /***
     * Closes the file
     *
     * @throws IOException If disk is not accessible for writing
     */
    public void close(int fileDescriptor) throws IOException {
        if (fileDescriptor != this.iNodeNumber){
            throw new IOException("FileSystem::close: file descriptor, "+
                    fileDescriptor + " does not match file descriptor " +
                    "of open file");
        }
        diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);
        this.iNodeForFile = null;
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
    }


    /**
     * Add your Javadoc documentation for this method
     */
    public String read(int fileDescriptor) throws IOException {
        // TODO: Replace this line with your code

        /* [valery]
             reads contents of file from its associated data blocks by traversing the inode file pointers and returns it
         */
        return null;
    }


    /**
     * Add your Javadoc documentation for this method
     */
    public void write(int fileDescriptor, String data) throws IOException {

        // TODO: Replace this line with your code

        /* [valery]
             split data into 512 bytes chunks and allocate a data block for each chunk
            update the inode with the correct pointers
         */
        allocateBlocksForFile(this.iNodeNumber, data.getBytes().length);

    }


    /**
     * this method will allocate the required number of blocks from the freeblock list and also update
     * the pointers of the inode
     */
    private int[] allocateBlocksForFile(int iNodeNumber, int numBytes)
            throws IOException {

        int numOfBlocksNeeded = (int) Math.ceil((double) numBytes / 512);
        int[] blockPointers = new int[numOfBlocksNeeded];
        int missing = numOfBlocksNeeded;
        int index = 0;
        int maximumSize = freeBlockList.getFreeBlockList().length * 8;

        System.out.println("Need to allocate " + numOfBlocksNeeded + " data blocks for iNodeNumber "+iNodeNumber);

        for(int i = 0; i < maximumSize; i++){
            if(isBlockFree(i)){
                blockPointers[index] = i;
                freeBlockList.allocateBlock(i);
                index++;
                missing--;
            }
            // found enough free blocks, no need to keep looking
            if(missing <= 0){
                break;
            }
        }

        printBlocksAllocated(iNodeNumber, blockPointers);

        // setting the pointers to the data blocks of the file
        for(int i = 0; i < blockPointers.length; i++){
            iNodeForFile.setBlockPointer(i, blockPointers[i]);
        }

        // setting the size to the amount of data blocks for the file
        iNodeForFile.setSize(blockPointers.length);

        // checking if block pointer values have ACTUALLY updated
        System.out.print("the block pointers allocated and updated for inodeNumber: " + iNodeNumber + " = [");
        for(int i = 0; i < blockPointers.length; i++){
            if(i == blockPointers.length-1){
                System.out.print(iNodeForFile.getBlockPointer(i));
            }
            else{
                System.out.print(iNodeForFile.getBlockPointer(i) + ", ");
            }
        }
        System.out.println("]");

        return blockPointers;
    }

    public boolean isBlockFree (int blockNumber){
        int blockNum = blockNumber / 8;
        int offset = blockNumber % 8;
        return (freeBlockList.getFreeBlockList()[blockNum] & (1 << offset)) == 0;
    }

    public void printBlocksAllocated(int iNodeNumber, int[] blockPointers){
        System.out.print("blocks allocated for iNodeNumber "+iNodeNumber+ " = [");
        for(int i = 0; i < blockPointers.length; i++){
            if (i == blockPointers.length-1){
                System.out.print(blockPointers[i]);
            }
            else {
                System.out.print(blockPointers[i] + ", ");
            }
        }
        System.out.print("]");
        System.out.println();
    }

    /*
    [valery] for testing purposes: to check if allocation is working properly for first 8 blocks of data
    this method will be deleted before submitting assignment
     */
    public void printFirst8Blocks(){
        for (int i = 0; i < 8; i++){
            System.out.println((freeBlockList.getFreeBlockList()[0] & (1 << i)) != 0);
        }
    }

    /**
     * get blocks method will get all the data blocks from the inode to be deleted
     * using those pointers, deallocate method will call deallocateBlock for each of them
     */
    private void deallocateBlocksForFile(int iNodeNumber) {
        // You may add any private method after this comment
        try{
            int[] blocks = getBlocks(iNodeNumber);
            System.out.print("blocks: [");
            for(int i = 0; i < blocks.length; i++){
                freeBlockList.deallocateBlock(blocks[i]);
                if(i == blocks.length-1){
                    System.out.print(blocks[i]);
                }
                else{
                    System.out.print(blocks[i] + ", ");
                }
            }
            System.out.println("] deleted for inode: "+iNodeNumber);
        } catch (IOException ioException){
            System.out.println("Unable to deallocate blocks for iNodeNumber " +iNodeNumber);
        }
    }

    /**
        Searches for all the data blocks associated with an inode and returns a list of those blocks
     **/
    private int[] getBlocks(int inodeNumber) throws IOException{
        INode tmpINode = null;
        int numOfDataBlocks = -1;
        for(int i = 0; i < Disk.NUM_INODES; i++){
            if(i == inodeNumber){
                tmpINode = diskDevice.readInode(i);
                numOfDataBlocks = tmpINode.getSize();
                break;
            }
        }

        int[] blocksToDelete = new int [numOfDataBlocks];

        if(numOfDataBlocks != -1){
            for(int i = 0; i < Disk.NUM_INODES; i++){
                if(i == inodeNumber){
                    tmpINode = diskDevice.readInode(i);
                    // get each data block that will be deleted
                    for(int j = 0; j < numOfDataBlocks; j++){
                        blocksToDelete[j] = tmpINode.getBlockPointer(j);
                    }
                }
            }
        }
        return blocksToDelete;
    }

    // <!-- For testing purposes --!>
    public int getNumberOfBlocksAllocated(){
        int result = 0;
        for (int i = 0; i < freeBlockList.getFreeBlockList().length * 8; i++){
            if(!isBlockFree(i)){
                result++;
            }
        }
        return result;
    }
}
