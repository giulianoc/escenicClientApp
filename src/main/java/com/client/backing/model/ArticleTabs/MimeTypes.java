package com.client.backing.model.ArticleTabs;

import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 17/12/15
 * Time: 04:24
 * To change this template use File | Settings | File Templates.
 */
public class MimeTypes {

    private static String imageMimeTypes = "|image/png|image/jpeg|image/gif|";
    private static String imageFileExtensions = "|png|jpg|jpeg|gif|";

    private static String videoMimeTypes = "|video/wmv|video/mp4|video/avi|video/webm|";
    private static String videoFileExtensions = "|wmv|mp4|mpeg|avi|webm|";

    private static String audioMimeTypes = "|audio/mp3|audio/mp4|audio/mpeg|audio/aac|";
    private static String audioFileExtensions = "|mp3|mp4|mpeg|aac|";
  /*
    audio:
                    <mime-type>audio/mpeg</mime-type>

                <mime-type>audio/mp3</mime-type>
                <mime-type>audio/mus</mime-type>
                <mime-type>audio/x-ms-wma</mime-type>
                <mime-type>audio/mp4</mime-type>
                <mime-type>audio/aac</mime-type>
                <mime-type>audio/x-mpeg-3</mime-type>
                <mime-type>audio/mpeg3</mime-type>
                <mime-type>audio/mpeg</mime-type>
                <mime-type>audio/x-wav</mime-type>
                <mime-type>audio/wav</mime-type>
                <mime-type>application/vnd.musician</mime-type>
                <mime-type>application/octet-stream</mime-type>
    video:
                <mime-type>video/mpeg</mime-type>
                <mime-type>video/wmv</mime-type>
                <mime-type>video/x-ms-wmv</mime-type>
                <mime-type>video/avi</mime-type>
                <mime-type>video/msvideo</mime-type>
                <mime-type>video/x-msvideo</mime-type>
                <mime-type>video/xmpg2</mime-type>
                <mime-type>application/x-troff-msvideo</mime-type>
                <mime-type>video/flv</mime-type>
                <mime-type>video/x-flv</mime-type>
                <mime-type>video/mp4</mime-type>
                <mime-type>application/mxf</mime-type>
                <mime-type>video/gxf</mime-type>
                <mime-type>video/x-m4v</mime-type>
                <mime-type>video/webm</mime-type>
                <mime-type>video/ogg</mime-type>
                <mime-type>video/quicktime</mime-type>
                <mime-type>application/octet-stream</mime-type>
     */

    public static String getMimeType(String fileName)
    {
        if (fileName.endsWith(".png"))
            return "image/png";
        else if (fileName.endsWith(".jpeg"))
            return "image/jpeg";
        else if (fileName.endsWith(".jpg"))
            return "image/jpeg";
        else if (fileName.endsWith(".gif"))
            return "image/gif";

        else if (fileName.endsWith(".wmv"))
            return "video/wmv";
        else if (fileName.endsWith(".mp4"))
            return "video/mp4";
        else if (fileName.endsWith(".avi"))
            return "video/avi";
        else if (fileName.endsWith(".webm"))
            return "video/webm";

        else if (fileName.endsWith(".mp3"))
            return "audio/mp3";
        else if (fileName.endsWith(".mp4"))
            return "audio/mp4";
        else if (fileName.endsWith(".mpeg"))
            return "audio/mpeg";
        else if (fileName.endsWith(".aac"))
            return "audio/aac";
        else
        {
            Logger.getLogger(MimeTypes.class).error("Unknown file extention. FileName: " + fileName);

            return null;
        }
    }

    public static String getImageFileExtensions()
    {
        return imageFileExtensions;
    }

    public static boolean isImageMimeType(String mimeType)
    {
        return imageMimeTypes.contains("|" + mimeType + "|");
    }

    public static boolean isImageFileExtension(String fileExtension)
    {
        return imageFileExtensions.contains("|" + fileExtension + "|");
    }

    public static String getVideoFileExtensions()
    {
        return videoFileExtensions;
    }

    public static boolean isVideoMimeType(String mimeType)
    {
        return videoMimeTypes.contains("|" + mimeType + "|");
    }

    public static boolean isVideoFileExtension(String fileExtension)
    {
        return videoFileExtensions.contains("|" + fileExtension + "|");
    }

    public static String getAudioFileExtensions()
    {
        return audioFileExtensions;
    }

    public static boolean isAudioMimeType(String mimeType)
    {
        return audioMimeTypes.contains("|" + mimeType + "|");
    }

    public static boolean isAudioFileExtension(String fileExtension)
    {
        return audioFileExtensions.contains("|" + fileExtension + "|");
    }

}
