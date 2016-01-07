//
//  SOSPicker.m
//  SyncOnSet
//
//  Created by Christopher Sullivan on 10/25/13.
//
//

#import "SOSPicker.h"


#import "GMImagePickerController.h"
#import "GMFetchItem.h"
#import "MBProgressHUD.h"

#define CDV_PHOTO_PREFIX @"cdv_photo_"

extern NSUInteger kGMImageMaxSeletedNumber;

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
  
    self.outputType = [[options objectForKey:@"outputType"] integerValue];
    BOOL allow_video = [[options objectForKey:@"allow_video" ] boolValue ];
    NSString * title = [options objectForKey:@"title"];
    NSString * message = [options objectForKey:@"message"];
    if (message == (id)[NSNull null]) {
      message = nil;
    }
    self.width = [[options objectForKey:@"width"] integerValue];
    self.height = [[options objectForKey:@"height"] integerValue];
    self.quality = [[options objectForKey:@"quality"] integerValue];

    self.callbackId = command.callbackId;
    [self launchGMImagePicker:allow_video title:title message:message];
}

- (void)launchGMImagePicker:(bool)allow_video title:(NSString *)title message:(NSString *)message
{
    GMImagePickerController *picker = [[GMImagePickerController alloc] init:allow_video];
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
    //popPC.sourceRect = nil;
    
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
    [picker.presentingViewController dismissViewControllerAnimated:YES completion:nil];
    NSLog(@"UIImagePickerController: User pressed cancel button");
}

#pragma mark - GMImagePickerControllerDelegate

- (void)assetsPickerController:(GMImagePickerController *)picker didFinishPickingAssets:(NSArray *)fetchArray
{
    [picker.presentingViewController dismissViewControllerAnimated:YES completion:nil];
    
    NSLog(@"GMImagePicker: User finished picking assets. Number of selected items is: %lu", (unsigned long)fetchArray.count);
    
    NSMutableArray * result_all = [[NSMutableArray alloc] init];
    __block NSMutableArray *resultStrings = [[NSMutableArray alloc] init];
    CGSize targetSize = CGSizeMake(self.width, self.height);
    NSFileManager* fileMgr = [[NSFileManager alloc] init];
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
    
    MBProgressHUD *progressHUD = [MBProgressHUD showHUDAddedTo:self.viewController.presentedViewController.view.superview
                                                      animated:YES];
    progressHUD.mode = MBProgressHUDModeDeterminate;
    progressHUD.dimBackground = YES;
    progressHUD.labelText = NSLocalizedStringFromTable(
                                                       @"picker.selection.processing",
                                                       @"GMImagePicker",
                                                       @"Loading"
                                                       );
    [progressHUD show: YES];
    dispatch_group_async(dispatchGroup, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        __block NSString* filePath;
        int i = 1;

        NSError* err = nil;
        __block NSData *imgData;
        // Index for tracking the current image
        int index = 0;
        // If image fetching fails then retry 3 times before giving up
        int retry = 3;
        do {
            PHAsset *asset = [fetchArray objectAtIndex:index];
            if (asset == nil) {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
            } else {
                __block UIImage *image;
                if (retry > 0) {
                    [manager requestImageDataForAsset:asset
                                  options:requestOptions
                                resultHandler:^(NSData *imageData,
                                                    NSString *dataUTI,
                                                    UIImageOrientation orientation,
                                                    NSDictionary *info) {
                                imgData = [imageData copy];
                                NSString* fullFilePath = [info objectForKey:@"PHImageFileURLKey"];
                                NSString* fileName = [[fullFilePath lastPathComponent] stringByDeletingPathExtension];
                                filePath = [NSString stringWithFormat:@"%@/%@.%@", docsPath, fileName, @"jpg"];
                            }];
                    retry--;
                    requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeFastFormat;
                } else {
                    retry = 3;
                    index++;
                    requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
                }
                
                if (imgData != nil) {
                    retry = 3;
                    requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
//                    do {
//                        filePath = [NSString stringWithFormat:@"%@/%@%03d.%@", docsPath, CDV_PHOTO_PREFIX, i++, @"jpg"];
//                    } while ([fileMgr fileExistsAtPath:filePath]);
                    
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
                            [resultStrings addObject:[[NSURL fileURLWithPath:filePath] absoluteString]];
                        }
                        data = nil;
                    }
//                    index = [NSNumber numberWithInt:[index intValue] + 1];
                    index++;
                }
            }
        } while (index < fetchArray.count);
        
        if (result == nil) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:resultStrings];
        }
    });
    
    dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
        if (nil == result) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:resultStrings];
        }
        
        progressHUD.progress = 1.f;
        [progressHUD hide:YES];
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    });
    
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


@end