//
//  SOSPicker.h
//  SyncOnSet
//
//  Created by Christopher Sullivan on 10/25/13.
//
//

#import <Cordova/CDVPlugin.h>


@interface SOSPicker : CDVPlugin <UINavigationControllerDelegate, UIScrollViewDelegate>

@property (copy)   NSString* callbackId;

- (void) getPictures:(CDVInvokedUrlCommand *)command;
- (UIImage*)imageByScalingNotCroppingForSize:(UIImage*)anImage toSize:(CGSize)frameSize;

@property (nonatomic, assign) NSInteger width;
@property (nonatomic, assign) NSInteger height;
@property (nonatomic, assign) NSInteger quality;
@property (nonatomic, strong) NSString* storage;
@property (nonatomic, assign) NSInteger outputType;
@property (nonatomic, assign) NSArray *preSelectedAssets;
@property (nonatomic, assign) BOOL allow_video;
@property (nonatomic, assign) NSString* title;
@property (nonatomic, assign) NSString* message;
@end
