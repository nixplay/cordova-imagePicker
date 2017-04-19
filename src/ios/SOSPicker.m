//
//  SOSPicker.m
//  SyncOnSet
//
//  Created by Christopher Sullivan on 10/25/13.
//
//

#import "SOSPicker.h"

#import "GMImagePickerController.h"
#import "MBProgressHUD.h"
#import <AssetsLibrary/AssetsLibrary.h>

#define CDV_PHOTO_PREFIX @"cdv_photo_"
#define SYSTEM_VERSION_LESS_THAN(v)                 ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedAscending)



typedef enum : NSUInteger {
    FILE_URI = 0,
    BASE64_STRING = 1
} SOSPickerOutputType;

@interface SOSPicker () <GMImagePickerControllerDelegate>
@end

@implementation SOSPicker

@synthesize callbackId;

- (void) getPictures:(CDVInvokedUrlCommand *)command {
    NSDictionary *options = [command.arguments objectAtIndex: 0];
    NSInteger maximumImagesCount = [[options objectForKey:@"maximumImagesCount"] integerValue];

    self.outputType = [[options objectForKey:@"outputType"] integerValue];
    self.allow_video = [[options objectForKey:@"allow_video" ] boolValue ];
    self.title = [options objectForKey:@"title"];
    self.message = [options objectForKey:@"message"];
    
    if (message == (id)[NSNull null]) {
      message = nil;
    }
    self.width = [[options objectForKey:@"width"] integerValue];
    self.height = [[options objectForKey:@"height"] integerValue];
    self.quality = [[options objectForKey:@"quality"] integerValue];


    self.preSelectedAssets = [options objectForKey:@"assets"];

    self.callbackId = command.callbackId;
    if ([PHObject class]) {
        PHAuthorizationStatus authStatus = [PHPhotoLibrary authorizationStatus];
        // Check if the user has access to photos
        if (authStatus == PHAuthorizationStatusDenied || authStatus == PHAuthorizationStatusRestricted) {
            [self showAuthorizationDialog];
        } else {
            [self launchGMImagePicker:self.allow_video title:self.title message:self.message];
        }
    } 
}

- (void)showAuthorizationDialog {
    // If iOS 8+, offer a link to the Settings app
    NSString* settingsButton = (&UIApplicationOpenSettingsURLString != NULL)
    ? NSLocalizedString(@"Settings", nil)
    : nil;

    // Denied; show an alert
    dispatch_async(dispatch_get_main_queue(), ^{
        [[[UIAlertView alloc] initWithTitle:[[NSBundle mainBundle]
                                             objectForInfoDictionaryKey:@"CFBundleDisplayName"]
                                    message:NSLocalizedString(@"Access to the camera roll has been prohibited; please enable it in the Settings app to continue.", nil)
                                   delegate:self
                          cancelButtonTitle:NSLocalizedString(@"OK", nil)
                          otherButtonTitles:settingsButton, nil] show];
    });
}

- (void) cleanupTempFiles:(CDVInvokedUrlCommand *)command {
    [self cleanupTempFiles];
}

- (void)launchGMImagePicker:(bool)allow_video title:(NSString *)title message:(NSString *)message
{
    GMImagePickerController *picker = [[GMImagePickerController alloc] init:allow_video withAssets: self.preSelectedAssets delegate:self];
    picker.delegate = self;
    picker.title = title;
    picker.customNavigationBarPrompt = message;
    picker.colsInPortrait = 3;
    picker.colsInLandscape = 5;
    picker.minimumInteritemSpacing = 2.0;
    picker.modalPresentationStyle = UIModalPresentationPopover;

    UIPopoverPresentationController *popPC = picker.popoverPresentationController;
    popPC.permittedArrowDirections = UIPopoverArrowDirectionAny;
    
    popPC.sourceView = picker.view;
    CGFloat width = [UIScreen mainScreen].bounds.size.width;
    CGFloat height = [UIScreen mainScreen].bounds.size.height;
    popPC.sourceRect = CGRectMake(width * 0.45, height * 0.65, 10, 10);
    [self.viewController showViewController:picker sender:nil];
}




- (UIImage*)imageByScalingNotCroppingForSize:(UIImage*)anImage toSize:(CGSize)frameSize
{
    UIImage* sourceImage = anImage;
    UIImage* newImage = nil;
    CGSize imageSize = sourceImage.size;
    CGFloat width = imageSize.width;
    CGFloat height = imageSize.height;
    CGFloat targetWidth = frameSize.width;
    CGFloat targetHeight = frameSize.height;
    CGFloat scaleFactor = 0.0;
    CGSize scaledSize = frameSize;

    if (CGSizeEqualToSize(imageSize, frameSize) == NO) {
        CGFloat widthFactor = targetWidth / width;
        CGFloat heightFactor = targetHeight / height;

        // opposite comparison to imageByScalingAndCroppingForSize in order to contain the image within the given bounds
        if (widthFactor == 0.0) {
            scaleFactor = heightFactor;
        } else if (heightFactor == 0.0) {
            scaleFactor = widthFactor;
        } else if (widthFactor > heightFactor) {
            scaleFactor = heightFactor; // scale to fit height
        } else {
            scaleFactor = widthFactor; // scale to fit width
        }
        scaledSize = CGSizeMake(floor(width * scaleFactor), floor(height * scaleFactor));
    }

    UIGraphicsBeginImageContext(scaledSize); // this will resize

    [sourceImage drawInRect:CGRectMake(0, 0, scaledSize.width, scaledSize.height)];

    newImage = UIGraphicsGetImageFromCurrentImageContext();
    if (newImage == nil) {
        NSLog(@"could not scale image");
    }

    // pop the context to get back to the default
    UIGraphicsEndImageContext();
    return newImage;
}


#pragma mark - UIImagePickerControllerDelegate


- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
{
    [picker.presentingViewController dismissViewControllerAnimated:YES completion:nil];
    NSLog(@"UIImagePickerController: User finished picking assets");
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    CDVPluginResult* pluginResult = nil;
    NSArray* emptyArray = [NSArray array];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:emptyArray];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];

    [picker.presentingViewController dismissViewControllerAnimated:YES completion:nil];
    NSLog(@"UIImagePickerController: User pressed cancel button");
}



#pragma mark - GMImagePickerControllerDelegate

- (void)assetsPickerController:(GMImagePickerController *)picker didFinishPickingAssets:(NSArray *)fetchArray
{
    [picker.presentingViewController dismissViewControllerAnimated:YES completion:nil];

    NSLog(@"GMImagePicker: User finished picking assets. Number of selected items is: %lu", (unsigned long)fetchArray.count);

    __block NSMutableArray *preSelectedAssets = [[NSMutableArray alloc] init];
    __block NSMutableArray *fileStrings = [[NSMutableArray alloc] init];
    __block NSMutableArray *livePhotoFileStrings = [[NSMutableArray alloc] init];
    
    __block NSMutableArray *invalidImages = [[NSMutableArray alloc] init];
    CGSize targetSize = CGSizeMake(self.width, self.height);
    NSString* docsPath = [NSTemporaryDirectory()stringByStandardizingPath];

    __block CDVPluginResult* result = nil;

    PHImageManager *manager = [PHImageManager defaultManager];
    PHImageRequestOptions *requestOptions;
    requestOptions = [[PHImageRequestOptions alloc] init];
    requestOptions.resizeMode   = PHImageRequestOptionsResizeModeExact;
    requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
    requestOptions.networkAccessAllowed = YES;

    // this one is key
    requestOptions.synchronous = true;

    dispatch_group_t dispatchGroup = dispatch_group_create();

    MBProgressHUD *progressHUD = [MBProgressHUD showHUDAddedTo:self.viewController.view
                                                      animated:YES];
    progressHUD.mode = MBProgressHUDModeIndeterminate;
    progressHUD.dimBackground = YES;
    progressHUD.labelText = NSLocalizedStringFromTable(
                                                       @"picker.selection.downloading",
                                                       @"GMImagePicker",
                                                       @"iCloudLoading"
                                                       );
    [progressHUD show: YES];
    dispatch_group_async(dispatchGroup, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        __block NSString* filePath;
        NSError* err = nil;
        __block NSData *imgData;
        // Index for tracking the current image
        __block int index = 0;
        // If image fetching fails then retry 3 times before giving up
        do {
            
            PHAsset *asset = [fetchArray objectAtIndex:index];
            NSString *localIdentifier;
            
            if(self.allow_video){
                PHVideoRequestOptions *options = [[PHVideoRequestOptions alloc] init];
                options.deliveryMode = PHVideoRequestOptionsDeliveryModeAutomatic;
                options.networkAccessAllowed = YES;
                [manager requestAVAssetForVideo:asset
                                        options:options
                                  resultHandler:^(AVAsset * _Nullable asset, AVAudioMix * _Nullable audioMix, NSDictionary * _Nullable info) {
                                      if([asset isKindOfClass:[AVURLAsset class]]){
                                          [fileStrings addObject: [[((AVURLAsset*)asset) URL] absoluteString] ];
                                          result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", invalidImages, @"invalidImages", nil]];
                                      }
                                      
                }];
                index++;
            }else{
                if (asset == nil) {
                    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                } else {
                    __block UIImage *image;
                    localIdentifier = [asset localIdentifier];
                    NSLog(@"localIdentifier: %@", localIdentifier);
                    
                    PHAssetResource *videoResource = nil;
                    NSArray *resourcesArray = [PHAssetResource assetResourcesForAsset:asset];
                    const NSInteger livePhotoAssetResourcesCount = 2;
                    const NSInteger videoPartIndex = 1;
                    
                    if (resourcesArray.count == livePhotoAssetResourcesCount) {
                        videoResource = resourcesArray[videoPartIndex];
                    }
                    
                    if (videoResource) {
                        NSString * const fileURLKey = @"_fileURL";
                        NSURL *videoURL = [videoResource valueForKey:fileURLKey];
//                        videoResource.assetLocalIdentifier
                        NSLog(@"videoURL %@",videoURL);
                        // load video url using AVKit or AVFoundation
                        
                        [livePhotoFileStrings addObject:videoResource.assetLocalIdentifier];
                    }
                    
                    [manager requestImageDataForAsset:asset
                                  options:requestOptions
                                resultHandler:^(NSData *imageData,
                                                    NSString *dataUTI,
                                                    UIImageOrientation orientation,
                                                    NSDictionary *info) {
                                if([dataUTI isEqualToString:@"public.png"] || [dataUTI isEqualToString:@"public.jpeg"] || [dataUTI isEqualToString:@"public.jpeg-2000"]) {
                                    imgData = [imageData copy];
                                    NSString* fullFilePath = [info objectForKey:@"PHImageFileURLKey"];
                                    NSLog(@"fullFilePath: %@: " , fullFilePath);
                                    NSString* fileName = [[localIdentifier componentsSeparatedByString:@"/"] objectAtIndex:0];
                                    filePath = [NSString stringWithFormat:@"%@/%@.%@", docsPath, fileName, @"jpg"];
                                } else {
                                    imgData = nil;
                                    [invalidImages addObject: localIdentifier];
                                    index++;
                                }
                            }];


                    requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeFastFormat;

                    if (imgData != nil) {
                        requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
                        @autoreleasepool {
                            NSData* data = nil;
                            if (self.width == 0 && self.height == 0) {
                                // no scaling required
                                if (self.quality == 100) {
                                    data = [imgData copy];
                                } else {
                                    image = [UIImage imageWithData:imgData];
                                    // resample first
                                    data = UIImageJPEGRepresentation(image, self.quality/100.0f);
                                }
                            } else {
                                image = [UIImage imageWithData:imgData];
                                // scale
                                UIImage* scaledImage = [self imageByScalingNotCroppingForSize:image toSize:targetSize];
                                data = UIImageJPEGRepresentation(scaledImage, self.quality/100.0f);
                            }
                            if (![data writeToFile:filePath options:NSAtomicWrite error:&err]) {
                                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                                break;
                            } else {
                                [fileStrings addObject:[[NSURL fileURLWithPath:filePath] absoluteString]];
                                [preSelectedAssets addObject: localIdentifier];
                            }
                            data = nil;
                        }
                        index++;
                    }
                }
            }
        } while (index < fetchArray.count);

        if (result == nil) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", livePhotoFileStrings, @"live_photos", invalidImages, @"invalidImages", nil]];
        }
    });

    dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
        if (nil == result) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", livePhotoFileStrings, @"live_photos",  invalidImages, @"invalidImages", nil]];
        }

        progressHUD.progress = 1.f;
        [progressHUD hide:YES];
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    });

}


- (NSString*)createDirectory:(NSString*)dir
{
    BOOL isDir = FALSE;
    NSFileManager *fileManager = [NSFileManager defaultManager];
    BOOL isDirExist = [fileManager fileExistsAtPath:dir isDirectory:&isDir];

    //If dir is not exist, create it
    if(!(isDirExist && isDir))
    {
        BOOL bCreateDir =[[NSFileManager defaultManager] createDirectoryAtPath:dir withIntermediateDirectories:YES attributes:nil error:nil];
        if (bCreateDir == NO)
        {
            NSLog(@"Failed to create Directory:%@", dir);
            return nil;
        }
    } else{
        //NSLog(@"Directory exist:%@", dir);
    }

    return dir;
}

- (NSString *)applicationDocumentsDirectory
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *basePath = ([paths count] > 0) ? [paths objectAtIndex:0] : nil;
    return basePath;
}

- (NSString *)getDraftsDirectory
{
    NSString *draftsDirectory = [[self applicationDocumentsDirectory] stringByAppendingPathComponent:@"drafts"];
    [self createDirectory:draftsDirectory];
    return draftsDirectory;
}


- (void)cleanupTempFiles {
    NSString* docsPath = [NSTemporaryDirectory()stringByStandardizingPath];
    NSFileManager *localFileManager=[[NSFileManager alloc] init];
    NSDirectoryEnumerator *dirEnum = [localFileManager enumeratorAtPath:docsPath];

    NSString *file;

    while ((file = [dirEnum nextObject])) {
        if([file.pathExtension isEqual: @"jpg"] || [file.pathExtension isEqual: @"jpeg" ] || [file.pathExtension isEqual: @"png"]) {
            NSString *filePath = [[docsPath stringByAppendingString:@"/"] stringByAppendingString:file];
            NSLog(@"Deleting file at %@", filePath);
            NSError* err = nil;
            [localFileManager removeItemAtPath:filePath
                                         error:&err];
            if(err) {
                NSLog(@"Delete returned error: %@", [err localizedDescription]);
            }
        }
    }

    NSString* docsPath2 = [self getDraftsDirectory];
    NSFileManager *localFileManager2=[[NSFileManager alloc] init];
    NSDirectoryEnumerator *dirEnum2 = [localFileManager2 enumeratorAtPath:docsPath2];

    while ((file = [dirEnum2 nextObject])) {
        if([file.pathExtension isEqual: @"jpg"] || [file.pathExtension isEqual: @"jpeg" ] || [file.pathExtension isEqual: @"png"]) {
            NSString *filePath = [[docsPath2 stringByAppendingString:@"/"] stringByAppendingString:file];
            NSLog(@"Deleting file at %@", filePath);
            NSError* err = nil;
            [localFileManager removeItemAtPath:filePath
                                         error:&err];
            if(err) {
                NSLog(@"Delete returned error: %@", [err localizedDescription]);
            }
        }
    }

    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:true];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

//Optional implementation:
-(void)assetsPickerControllerDidCancel:(GMImagePickerController *)picker
{
    CDVPluginResult* pluginResult = nil;
    NSArray* emptyArray = [NSArray array];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:emptyArray];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    NSLog(@"GMImagePicker: User pressed cancel button");
}

// Delegate for camera roll permission UIAlertView
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    // If Settings button (on iOS 8), open the settings app
    if (buttonIndex == 1) {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString]];
    }

    // Dismiss the view
    [self.viewController dismissViewControllerAnimated:YES completion:nil];

    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"has no access to camera"];   // error callback expects string ATM

    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];

}

-(BOOL) shouldSelectAllAlbumCell{
    return YES;
}
 -(NSString*) controllerTitle{
    return self.title;
 }
 -(NSString*) controllerCustomNavigationBarPrompt{
    return self.message;
 }
@end
