//
// Created by 顏培峻 on 2019/4/1.
//

#include <stdio.h>
#include <stdlib.h>
#include <linux/usbdevice_fs.h>
#include <sys/ioctl.h>
#include <cstring>

// We have 32 URBs
#define NUM_URBS       32
#define BUFFER_SIZE    16384

char *getURBs(int fd, int ep)
{
    struct usbdevfs_urb urbs[NUM_URBS];
    struct usbdevfs_bulktransfer bt;
    int len = 307200;
    int sizeCount = len;
    unsigned int urb_num = 0;

    // Allocate buffer for image
    char *buf = (char *)malloc(len * sizeof(char));

    /* Send out initial URBs */
    memset(urbs, 0, sizeof urbs);
    for (unsigned int i = 0; i < NUM_URBS; i++) {
        urbs[i].type = USBDEVFS_URB_TYPE_BULK;
        urbs[i].endpoint = ep;
        urbs[i].buffer = buf + (i * BUFFER_SIZE);
        urbs[i].buffer_length = (sizeCount < BUFFER_SIZE) ? sizeCount : BUFFER_SIZE;
        urbs[i].actual_length = (sizeCount < BUFFER_SIZE) ? sizeCount : BUFFER_SIZE;

        if (sizeCount > BUFFER_SIZE)
            sizeCount -= BUFFER_SIZE;

        if (ioctl(fd, USBDEVFS_SUBMITURB, &urbs[i]) < 0) {
            free(buf);
            return NULL;
        }
    }

    /* Wait for completions */
    while(urb_num < NUM_URBS) {

        struct usbdevfs_urb *urb;

        if (ioctl(fd, USBDEVFS_REAPURB, &urb) < 0) {
            free(buf);
            return NULL;
        }

        // Completed early
        if (urb->actual_length < BUFFER_SIZE)
            break;

        urb_num++;
    }

    return buf;
}
