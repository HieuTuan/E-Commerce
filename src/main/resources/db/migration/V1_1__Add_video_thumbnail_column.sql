-- Add video_thumbnail column to product_images table
ALTER TABLE product_images 
ADD COLUMN video_thumbnail VARCHAR(255) NULL;

-- Add comment for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Filename of the video thumbnail image', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'product_images', 
    @level2type = N'COLUMN', @level2name = N'video_thumbnail';