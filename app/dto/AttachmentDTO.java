package dto;

public class AttachmentDTO
{
    public String name;
    public String extension;
    public String path;
    public Long size;

    public AttachmentDTO(String name, String extension, String path, Long size)
    {
        this.name = name;
        this.extension = extension;
        this.path = path;
        this.size = size;
    }
}