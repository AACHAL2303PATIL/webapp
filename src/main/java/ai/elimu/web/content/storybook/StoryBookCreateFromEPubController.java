package ai.elimu.web.content.storybook;

import ai.elimu.dao.ImageDao;
import ai.elimu.dao.StoryBookChapterDao;
import ai.elimu.dao.StoryBookDao;
import ai.elimu.dao.StoryBookParagraphDao;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import ai.elimu.model.content.StoryBook;
import ai.elimu.model.content.StoryBookChapter;
import ai.elimu.model.content.StoryBookParagraph;
import ai.elimu.model.content.multimedia.Image;
import ai.elimu.model.enums.content.ImageFormat;
import ai.elimu.util.ImageColorHelper;
import ai.elimu.util.ImageHelper;
import ai.elimu.util.epub.EPubChapterExtractionHelper;
import ai.elimu.util.epub.EPubImageExtractionHelper;
import ai.elimu.util.epub.EPubMetadataExtractionHelper;
import ai.elimu.util.epub.EPubParagraphExtractionHelper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;

@Controller
@RequestMapping("/content/storybook/create-from-epub")
public class StoryBookCreateFromEPubController {
    
    private final Logger logger = Logger.getLogger(getClass());
    
    @Autowired
    private StoryBookDao storyBookDao;
    
    @Autowired
    private ImageDao imageDao;
    
    @Autowired
    private StoryBookChapterDao storyBookChapterDao;
    
    @Autowired
    private StoryBookParagraphDao storyBookParagraphDao;

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest(Model model) {
    	logger.info("handleRequest");
        
        StoryBook storyBook = new StoryBook();
        model.addAttribute("storyBook", storyBook);

        return "content/storybook/create-from-epub";
    }
    
    @RequestMapping(method = RequestMethod.POST)
    public String handleSubmit(
            StoryBook storyBook,
            @RequestParam("bytes") MultipartFile multipartFile,
            BindingResult result,
            Model model,
            HttpSession session
    ) throws IOException {
    	logger.info("handleSubmit");
        
        Image storyBookCoverImage = null;
        
        List<StoryBookChapter> storyBookChapters = new ArrayList<>();
        
        List<StoryBookParagraph> storyBookParagraphs = new ArrayList<>();
        
        if (multipartFile.isEmpty()) {
            result.rejectValue("bytes", "NotNull");
        } else {
            String contentType = multipartFile.getContentType();
            logger.info("contentType: " + contentType);
            
            String name = multipartFile.getName();
            logger.info("name: " + name);
            
            String originalFilename = multipartFile.getOriginalFilename();
            logger.info("originalFilename: " + originalFilename);
            
            long size = multipartFile.getSize();
            logger.info("size: " + size + " (" + (size / 1024) + "kB)");
            
            byte[] ePubBytes = multipartFile.getBytes();
            logger.info("ePubBytes.length: " + (ePubBytes.length / 1024 / 1024) + "MB");

            List<File> filesInEPub = unzipFiles(ePubBytes, originalFilename);
            logger.info("filesInEPub.size(): " + filesInEPub.size());

            // Extract the ePUB's metadata from its OPF file
            File opfFile = null;
            for (File file : filesInEPub) {
                if (file.getName().endsWith(".opf")) {
                    opfFile = file;
                }
            }
            logger.info("opfFile: \"" + opfFile + "\"");
            if (opfFile == null) {
                throw new IllegalArgumentException("The OPF file was not found");
            } else {
                String title = EPubMetadataExtractionHelper.extractTitleFromOpfFile(opfFile);
                logger.info("title: \"" + title + "\"");
                storyBook.setTitle(title);

                String description = EPubMetadataExtractionHelper.extractDescriptionFromOpfFile(opfFile);
                logger.info("description: \"" + description + "\"");
                if (StringUtils.isNotBlank(description)) {
                    logger.info("description.length(): " + description.length());
                    if (description.length() > 1024) {
                        description = description.substring(0, 1023);
                    }
                    storyBook.setDescription(description);
                }

                storyBookCoverImage = new Image();
                String coverImageReference = EPubMetadataExtractionHelper.extractCoverImageReferenceFromOpfFile(opfFile);
                logger.info("coverImageReference: " + coverImageReference);
                File coverImageFile = new File(opfFile.getParent(), coverImageReference);
                logger.info("coverImageFile: " + coverImageFile);
                logger.info("coverImageFile.exists(): " + coverImageFile.exists());
                URI coverImageUri = coverImageFile.toURI();
                logger.info("coverImageUri: " + coverImageUri);
                byte[] coverImageBytes = IOUtils.toByteArray(coverImageUri);
                storyBookCoverImage.setBytes(coverImageBytes);
                byte[] headerBytes = Arrays.copyOfRange(coverImageBytes, 0, 6);
                byte[] gifHeader87a = {71, 73, 70, 56, 55, 97}; // "GIF87a"
                byte[] gifHeader89a = {71, 73, 70, 56, 57, 97}; // "GIF89a"
                if (Arrays.equals(gifHeader87a, headerBytes) || Arrays.equals(gifHeader89a, headerBytes)) {
                    storyBookCoverImage.setContentType("image/gif");
                    storyBookCoverImage.setImageFormat(ImageFormat.GIF);
                } else if (coverImageFile.getName().toLowerCase().endsWith(".png")) {
                    storyBookCoverImage.setContentType("image/png");
                    storyBookCoverImage.setImageFormat(ImageFormat.PNG);
                } else if (coverImageFile.getName().toLowerCase().endsWith(".jpg") || coverImageFile.getName().toLowerCase().endsWith(".jpeg")) {
                    storyBookCoverImage.setContentType("image/jpg");
                    storyBookCoverImage.setImageFormat(ImageFormat.JPG);
                } else if (coverImageFile.getName().toLowerCase().endsWith(".gif")) {
                    storyBookCoverImage.setContentType("image/gif");
                    storyBookCoverImage.setImageFormat(ImageFormat.GIF);
                }
                try {
                    int[] dominantColor = ImageColorHelper.getDominantColor(storyBookCoverImage.getBytes());
                    storyBookCoverImage.setDominantColor("rgb(" + dominantColor[0] + "," + dominantColor[1] + "," + dominantColor[2] + ")");
                } catch (NullPointerException ex) {
                    // javax.imageio.IIOException: Unsupported Image Type
                }
                if (storyBookCoverImage.getImageFormat() != ImageFormat.GIF) {
                    // Reduce size if large image
                    int imageWidth = ImageHelper.getWidth(coverImageBytes);
                    logger.info("imageWidth: " + imageWidth + "px");
                    if (imageWidth > ImageHelper.MINIMUM_WIDTH) {
                        coverImageBytes = ImageHelper.scaleImage(coverImageBytes, ImageHelper.MINIMUM_WIDTH);
                        storyBookCoverImage.setBytes(coverImageBytes);
                    }
                }
            }

            // Extract the ePUB's chapters
            File tableOfContentsFile = null;
            for (File file : filesInEPub) {
                if (file.getName().startsWith("toc.")) {
                    tableOfContentsFile = file;
                }
            }
            logger.info("tableOfContentsFile: \"" + tableOfContentsFile + "\"");
            if (tableOfContentsFile == null) {
                throw new IllegalArgumentException("The TOC file was not found");
            } else {
                List<String> chapterReferences = null;
                if (tableOfContentsFile.getName().endsWith(".xhtml")) {
                    // StoryBookProvider#GLOBAL_DIGITAL_LIBRARY or StoryBookProvider#LETS_READ_ASIA
                    chapterReferences = EPubChapterExtractionHelper.extractChapterReferencesFromTableOfContentsFile(tableOfContentsFile);
                } else if (tableOfContentsFile.getName().endsWith(".ncx")) {
                    // StoryBookProvider#STORYWEAVER
                    chapterReferences = EPubChapterExtractionHelper.extractChapterReferencesFromTableOfContentsFileNcx(tableOfContentsFile);
                }
                logger.info("chapterReferences.size(): " + chapterReferences.size());

                // Extract each chapter's image (if any) and paragraphs
                for (String chapterReference : chapterReferences) {
                    logger.info("chapterReference: \"" + chapterReference + "\"");
                    File chapterFile = new File(opfFile.getParent(), chapterReference);
                    logger.info("chapterFile: \"" + chapterFile + "\"");
                    StoryBookChapter storyBookChapter = new StoryBookChapter();
                    storyBookChapter.setSortOrder(storyBookChapters.size());
                    storyBookChapters.add(storyBookChapter);

                    String chapterImageReference = EPubImageExtractionHelper.extractImageReferenceFromChapterFile(chapterFile);
                    logger.info("chapterImageReference: " + chapterImageReference);
                    if (StringUtils.isNotBlank(chapterImageReference)) {
                        File chapterImageFile = null;
                        if (chapterImageReference.startsWith("http://") || chapterImageReference.startsWith("https://")) {
                            // Download the file
                            
                            URL sourceUrl = new URL(chapterImageReference);
                            
                            String tmpDir = System.getProperty("java.io.tmpdir");
                            logger.info("tmpDir: " + tmpDir);
                            File tmpDirElimuAi = new File(tmpDir, "elimu-ai");
                            logger.info("tmpDirElimuAi: " + tmpDirElimuAi);
                            logger.info("tmpDirElimuAi.mkdir(): " + tmpDirElimuAi.mkdir());
                            chapterImageFile = new File(tmpDirElimuAi, "chapter-image");
                            
                            logger.warn("Downloading image from " + sourceUrl + " and storing at " + chapterImageFile);
                            int connectionTimeout = 1000 * 10; // 1000 milliseconds x 10
                            int readTimeout = 1000 * 10; // 1000 milliseconds x 10
                            FileUtils.copyURLToFile(sourceUrl, chapterImageFile, connectionTimeout, readTimeout);
                        } else {
                            chapterImageFile = new File(chapterFile.getParent(), chapterImageReference);
                        }
                        logger.info("chapterImageFile: " + chapterImageFile);
                        logger.info("chapterImageFile.exists(): " + chapterImageFile.exists());
                        URI chapterImageUri = chapterImageFile.toURI();
                        logger.info("chapterImageUri: " + chapterImageUri);
                        byte[] chapterImageBytes = IOUtils.toByteArray(chapterImageUri);
                        Image chapterImage = new Image();
                        chapterImage.setBytes(chapterImageBytes);
                        byte[] headerBytes = Arrays.copyOfRange(chapterImageBytes, 0, 6);
                        byte[] gifHeader87a = {71, 73, 70, 56, 55, 97}; // "GIF87a"
                        byte[] gifHeader89a = {71, 73, 70, 56, 57, 97}; // "GIF89a"
                        if (Arrays.equals(gifHeader87a, headerBytes) || Arrays.equals(gifHeader89a, headerBytes)) {
                            chapterImage.setContentType("image/gif");
                            chapterImage.setImageFormat(ImageFormat.GIF);
                        } else if (chapterImageFile.getName().toLowerCase().endsWith(".png")) {
                            chapterImage.setContentType("image/png");
                            chapterImage.setImageFormat(ImageFormat.PNG);
                        } else if (chapterImageFile.getName().toLowerCase().endsWith(".jpg") || chapterImageFile.getName().toLowerCase().endsWith(".jpeg")) {
                            chapterImage.setContentType("image/jpg");
                            chapterImage.setImageFormat(ImageFormat.JPG);
                        } else if (chapterImageFile.getName().toLowerCase().endsWith(".gif")) {
                            chapterImage.setContentType("image/gif");
                            chapterImage.setImageFormat(ImageFormat.GIF);
                        }
                        try {
                            int[] dominantColor = ImageColorHelper.getDominantColor(chapterImage.getBytes());
                            chapterImage.setDominantColor("rgb(" + dominantColor[0] + "," + dominantColor[1] + "," + dominantColor[2] + ")");
                        } catch (NullPointerException ex) {
                            // javax.imageio.IIOException: Unsupported Image Type
                        }
                        if (chapterImage.getImageFormat() != ImageFormat.GIF) {
                            // Reduce size if large image
                            int imageWidth = ImageHelper.getWidth(chapterImageBytes);
                            logger.info("imageWidth: " + imageWidth + "px");
                            if (imageWidth > ImageHelper.MINIMUM_WIDTH) {
                                chapterImageBytes = ImageHelper.scaleImage(chapterImageBytes, ImageHelper.MINIMUM_WIDTH);
                                chapterImage.setBytes(chapterImageBytes);
                            }
                        }
                        storyBookChapter.setImage(chapterImage);
                    }

                    List<String> paragraphs = EPubParagraphExtractionHelper.extractParagraphsFromChapterFile(chapterFile);
                    logger.info("paragraphs.size(): " + paragraphs.size());
                    for (int i = 0; i < paragraphs.size(); i++) {
                        String paragraph = paragraphs.get(i);
                        logger.info("paragraph: \"" + paragraph + "\"");
                        logger.info("paragraph.length(): " + paragraph.length());

                        StoryBookParagraph storyBookParagraph = new StoryBookParagraph();
                        storyBookParagraph.setStoryBookChapter(storyBookChapter);
                        storyBookParagraph.setSortOrder(i);

                        if (paragraph.length() > 1024) {
                            logger.warn("Reducing the length of the paragraph to its initial 1,024 characters.");
                            paragraph = paragraph.substring(0, 1023);
                        }
                        storyBookParagraph.setOriginalText(paragraph);

                        // Note: updating the paragraph's list of Words is handled by the ParagraphWordScheduler

                        storyBookParagraphs.add(storyBookParagraph);
                    }
                }
            }
        }
        
        if (result.hasErrors()) {
            return "content/storybook/create-from-epub";
        } else {
            // Store the StoryBook in the database
            storyBook.setTimeLastUpdate(Calendar.getInstance());
            storyBookDao.create(storyBook);
            
            // Store the StoryBook's cover image in the database, and assign it to the StoryBook
            storyBookCoverImage.setTitle("storybook-" + storyBook.getId() + "-cover");
            imageDao.create(storyBookCoverImage);
            storyBook.setCoverImage(storyBookCoverImage);
            storyBookDao.update(storyBook);
            
            // Store the StoryBookChapters in the database
            for (StoryBookChapter storyBookChapter : storyBookChapters) {
                storyBookChapter.setStoryBook(storyBook);
                
                // Store the chapter's image (if any)
                Image chapterImage = storyBookChapter.getImage();
                if (chapterImage != null) {
                    chapterImage.setTitle("storybook-" + storyBook.getId() + "-ch-" + (storyBookChapter.getSortOrder() + 1));
                    imageDao.create(chapterImage);
                }
                
                // Get the paragraphs associated with this chapter
                List<StoryBookParagraph> storyBookParagraphsAssociatedWithChapter = new ArrayList<>();
                for (StoryBookParagraph storyBookParagraph : storyBookParagraphs) {
                    if (storyBookParagraph.getStoryBookChapter().getSortOrder().equals(storyBookChapter.getSortOrder())) {
                        storyBookParagraphsAssociatedWithChapter.add(storyBookParagraph);
                    }
                }
                logger.info("storyBookParagraphsAssociatedWithChapter.size(): " + storyBookParagraphsAssociatedWithChapter.size());
                
                // Only store the chapter if it has an image or at least one paragraph
                if ((chapterImage != null) || (!storyBookParagraphsAssociatedWithChapter.isEmpty())) {
                    storyBookChapterDao.create(storyBookChapter);
                }
                
                // Store the chapter's paragraphs in the database
                for (StoryBookParagraph storyBookParagraph : storyBookParagraphsAssociatedWithChapter) {
                    storyBookParagraph.setStoryBookChapter(storyBookChapter);
                    storyBookParagraphDao.create(storyBookParagraph);
                }
            }
            
            return "redirect:/content/storybook/edit/" + storyBook.getId();
        }
    }
    
    /**
     * See http://www.mkyong.com/spring-mvc/spring-mvc-failed-to-convert-property-value-in-file-upload-form/
     * <p></p>
 Fixes this error message:
 "Cannot convert value of type [org.springframework.web.multipart.support.StandardMultipartHttpServletRequest$StandardMultipartFile] to required type [byte] for property 'ePubBytes[0]'"
     */
    @InitBinder
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
    	logger.info("initBinder");
    	binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }
    
    /**
     * Unzip the contents of the ePUB file to a temporary folder.
     */
    private List<File> unzipFiles(byte[] ePubBytes, String originalFilename) {
        logger.info("unzipFiles");
        
        List<File> unzippedFiles = new ArrayList<>();
        
        String tmpDir = System.getProperty("java.io.tmpdir");
        logger.info("tmpDir: " + tmpDir);
        File tmpDirElimuAi = new File(tmpDir, "elimu-ai");
        logger.info("tmpDirElimuAi: " + tmpDirElimuAi);
        logger.info("tmpDirElimuAi.mkdir(): " + tmpDirElimuAi.mkdir());
        File unzipDestinationDirectory = new File(tmpDirElimuAi, originalFilename.replace(" ", "_") + "_unzipped");
        logger.info("unzipDestinationDirectory: " + unzipDestinationDirectory);
        logger.info("unzipDestinationDirectory.mkdir(): " + unzipDestinationDirectory.mkdir());
        byte[] buffer = new byte[1024];
        try {
            InputStream inputStream = new ByteArrayInputStream(ePubBytes);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry zipEntry = null;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                logger.info("zipEntry: " + zipEntry);
                
                // E.g. unzipDestinationDirectory + "/" + "META-INF/container.xml"
                File unzipDestinationFile = new File(unzipDestinationDirectory + File.separator + zipEntry.toString());
                logger.info("unzipDestinationFile: " + unzipDestinationFile);
                
                // Create intermediate folders if they do not already exist, e.g. "META-INF/", "content/" or "OEBPS/"
                File parentDirectory = unzipDestinationFile.getParentFile();
                logger.info("parentDirectory: " + parentDirectory);
                if (!parentDirectory.exists()) {
                    boolean parentDirectoriesWereCreated = parentDirectory.mkdirs();
                    logger.info("parentDirectory.mkdirs(): " + parentDirectoriesWereCreated);
                }
                
                // Write file to disk
                FileOutputStream fileOutputStream = new FileOutputStream(unzipDestinationFile);
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.close();
                
                logger.info("unzipDestinationFile.exists(): " + unzipDestinationFile.exists());
                unzippedFiles.add(unzipDestinationFile);
            }
            zipInputStream.close();
            inputStream.close();
        } catch (FileNotFoundException ex) {
            logger.error(null, ex);
        } catch (IOException ex) {
            logger.error(null, ex);
        }
        
        return unzippedFiles;
    }
}
