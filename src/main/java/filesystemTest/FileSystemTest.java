package filesystemTest;
import filesystem.FileSystem;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class FileSystemTest {
    @Test
    void allocateBlocksForFileTest01() {

        try{
            int NUM_LINES = 3;
            String testData = "This is some text ";
            FileSystem fs = new FileSystem();
            String fileNameBase = "file";
            String fileName = null;
            String theMessage = null;

            for (int i= 0; i < NUM_LINES; i++) {
                fileName = new String(fileNameBase + i + "." + "txt");
                int fd = fs.create(fileName);
                theMessage = new String();
                for (int j= 0; j < i+1; j++) {
                    theMessage = theMessage.concat(testData + j + ".  ");
                }
                fs.write(fd, theMessage);
                fs.close(fd);
            }
            //the first 3 files are small and each will get 1 data block so 3 in total
            int expected = 3;
            int actual = fs.getNumberOfBlocksAllocated();
            assertEquals(expected, actual);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

    }

    @Test
    void allocateBlocksForFileTest02() {

        try{
            int NUM_LINES = 42;
            String testData = "This is some text ";
            FileSystem fs = new FileSystem();
            String fileNameBase = "file";
            String fileName = null;
            String theMessage = null;

            for (int i= 0; i < NUM_LINES; i++) {
                fileName = new String(fileNameBase + i + "." + "txt");
                int fd = fs.create(fileName);
                theMessage = new String();
                for (int j= 0; j < i+1; j++) {
                    theMessage = theMessage.concat(testData + j + ".  ");
                }
                fs.write(fd, theMessage);
                fs.close(fd);
            }
            // the first 22 files should get 1 block data each and the rest of them will get 2 each
            int expected = 22 + 20*2;
            int actual = fs.getNumberOfBlocksAllocated();
            assertEquals(expected, actual);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void deallocateBlocksForFileTest01() {

        try{
            int NUM_LINES = 42;
            String testData = "This is some text ";
            FileSystem fs = new FileSystem();
            String fileNameBase = "file";
            String fileName = null;
            String theMessage = null;

            for (int i= 0; i < NUM_LINES; i++) {
                fileName = new String(fileNameBase + i + "." + "txt");
                int fd = fs.create(fileName);
                theMessage = new String();
                for (int j= 0; j < i+1; j++) {
                    theMessage = theMessage.concat(testData + j + ".  ");
                }
                fs.write(fd, theMessage);
                fs.close(fd);
            }
            /**
             * delete every 2nd file
             */
            for (int i= 0 ; i < NUM_LINES; i+=2) {
                fileName = new String(fileNameBase + i + "." + "txt");
                fs.delete(fileName);
            }
            // first 22 files will get 1 data block and the rest gets 2, half will be deleted
            int expected = (22 + 20*2)/2;
            int actual = fs.getNumberOfBlocksAllocated();
            assertEquals(expected, actual);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void deallocateBlocksForFileTest02() {

        try{
            int NUM_LINES = 10;
            String testData = "This is some text ";
            FileSystem fs = new FileSystem();
            String fileNameBase = "file";
            String fileName = null;
            String theMessage = null;

            for (int i= 0; i < NUM_LINES; i++) {
                fileName = new String(fileNameBase + i + "." + "txt");
                int fd = fs.create(fileName);
                theMessage = new String();
                for (int j= 0; j < i+1; j++) {
                    theMessage = theMessage.concat(testData + j + ".  ");
                }
                fs.write(fd, theMessage);
                fs.close(fd);
            }
            /**
             * delete every 2nd file
             */
            for (int i= 0 ; i < NUM_LINES; i+=2) {
                fileName = new String(fileNameBase + i + "." + "txt");
                fs.delete(fileName);
            }
            // the first 10 files should get 1 block data each and 5 of them will be deleted
            int expected = (10 / 2);
            int actual = fs.getNumberOfBlocksAllocated();
            assertEquals(expected, actual);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
