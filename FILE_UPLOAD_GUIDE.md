# File Upload Configuration Guide

## Development Setup

### Backend (Spring Boot)
The application is configured to handle file uploads up to 5MB.

**File Storage:**
- Files are stored in: `uploads/avatars/`
- Files are served at: `http://localhost:8080/uploads/avatars/{filename}`

### Frontend (React)
- Upload API: `/api/teachers/profile/upload-avatar`
- Supports: PNG, JPG, JPEG, GIF, WEBP
- Max size: 5MB

## Production Deployment

### Option 1: Local File Storage (Simple)

1. **Create uploads directory on server:**
   ```bash
   mkdir -p /var/www/edu360/uploads/avatars
   chmod 755 /var/www/edu360/uploads
   ```

2. **Update application.properties:**
   ```properties
   app.upload.dir=/var/www/edu360/uploads
   app.upload.base-url=https://yourdomain.com/uploads
   ```

3. **Configure Nginx to serve files:**
   ```nginx
   location /uploads/ {
       alias /var/www/edu360/uploads/;
       expires 30d;
       add_header Cache-Control "public, immutable";
   }
   ```

### Option 2: Cloud Storage (Recommended for Production)

#### AWS S3
1. **Add dependencies to pom.xml:**
   ```xml
   <dependency>
       <groupId>com.amazonaws</groupId>
       <artifactId>aws-java-sdk-s3</artifactId>
       <version>1.12.x</version>
   </dependency>
   ```

2. **Update application.properties:**
   ```properties
   cloud.aws.credentials.access-key=YOUR_ACCESS_KEY
   cloud.aws.credentials.secret-key=YOUR_SECRET_KEY
   cloud.aws.region.static=ap-southeast-1
   cloud.aws.s3.bucket=edu360-uploads
   ```

3. **Create S3Service.java** (implementation provided separately)

#### Azure Blob Storage
1. **Add dependencies:**
   ```xml
   <dependency>
       <groupId>com.azure</groupId>
       <artifactId>azure-storage-blob</artifactId>
       <version>12.x.x</version>
   </dependency>
   ```

2. **Update application.properties:**
   ```properties
   azure.storage.connection-string=YOUR_CONNECTION_STRING
   azure.storage.container-name=edu360-uploads
   ```

### Option 3: CDN Integration

For better performance, integrate with CDN:
- CloudFlare
- AWS CloudFront
- Azure CDN

## Environment Variables

Set these environment variables in production:

```bash
# File upload
UPLOAD_DIR=/var/www/edu360/uploads
UPLOAD_BASE_URL=https://yourdomain.com/uploads

# Or for cloud storage
AWS_ACCESS_KEY=xxx
AWS_SECRET_KEY=xxx
AWS_S3_BUCKET=edu360-uploads
```

## Security Considerations

1. **File validation** - Only accept image files
2. **File size limits** - Max 5MB configured
3. **Filename sanitization** - UUID-based naming prevents conflicts
4. **Virus scanning** - Consider adding ClamAV integration
5. **CDN security** - Use signed URLs for sensitive content

## Backup Strategy

### Local Storage
```bash
# Daily backup script
rsync -av /var/www/edu360/uploads/ /backup/edu360/uploads/
```

### Cloud Storage
- S3: Enable versioning and lifecycle policies
- Azure: Enable soft delete and geo-redundancy

## Monitoring

Track these metrics:
- Upload success/failure rate
- Average file size
- Storage usage
- CDN bandwidth usage

## Migration from Development to Production

1. **Backup current files:**
   ```bash
   tar -czf uploads_backup.tar.gz uploads/
   ```

2. **Upload to cloud storage:**
   ```bash
   aws s3 sync uploads/ s3://edu360-uploads/
   ```

3. **Update database URLs:**
   ```sql
   UPDATE teachers 
   SET avatar_url = REPLACE(avatar_url, 'http://localhost:8080', 'https://yourdomain.com')
   WHERE avatar_url LIKE 'http://localhost:8080%';
   ```

4. **Test upload functionality:**
   - Upload new avatar
   - Verify URL is correct
   - Check file is accessible

## Troubleshooting

### File not accessible
- Check directory permissions
- Verify nginx/apache configuration
- Check firewall rules

### Upload fails
- Check file size limits
- Verify CORS configuration
- Check disk space

### Performance issues
- Enable CDN
- Add caching headers
- Compress images on upload
