// Copyright 2010, Shuo Chen.  All rights reserved.
// http://code.google.com/p/muduo/
//
// Use of this source code is governed by a BSD-style license
// that can be found in the License file.

// Author: Shuo Chen (chenshuo at chenshuo dot com)


#include "Types.h"
#include "Endian.h"

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>  // snprintf
#include <strings.h>  // bzero
#include <sys/socket.h>
#include <sys/uio.h>  // readv
#include <unistd.h>
#include <cstring>
#include "SocketsOps.h"

using namespace muduo;
using namespace muduo::net;

namespace {

    typedef struct sockaddr SA;


#if VALGRIND || defined (NO_ACCEPT4)
    void setNonBlockAndCloseOnExec(int sockfd)
    {
      // non-block
      int flags = ::fcntl(sockfd, F_GETFL, 0);
      flags |= O_NONBLOCK;
      int ret = ::fcntl(sockfd, F_SETFL, flags);
      // FIXME check

      // close-on-exec
      flags = ::fcntl(sockfd, F_GETFD, 0);
      flags |= FD_CLOEXEC;
      ret = ::fcntl(sockfd, F_SETFD, flags);
      // FIXME check

      (void)ret;
    }
#endif

}

const struct sockaddr *sockets::sockaddr_cast(const struct sockaddr_in6 *addr) {
    return static_cast<const struct sockaddr *>(implicit_cast<const void *>(addr));
}

struct sockaddr *sockets::sockaddr_cast(struct sockaddr_in6 *addr) {
    return static_cast<struct sockaddr *>(implicit_cast<void *>(addr));
}

const struct sockaddr *sockets::sockaddr_cast(const struct sockaddr_in *addr) {
    return static_cast<const struct sockaddr *>(implicit_cast<const void *>(addr));
}

const struct sockaddr_in *sockets::sockaddr_in_cast(const struct sockaddr *addr) {
    return static_cast<const struct sockaddr_in *>(implicit_cast<const void *>(addr));
}

const struct sockaddr_in6 *sockets::sockaddr_in6_cast(const struct sockaddr *addr) {
    return static_cast<const struct sockaddr_in6 *>(implicit_cast<const void *>(addr));
}

int sockets::createNonblockingOrDie(sa_family_t family) {
#if VALGRIND
    int sockfd = ::socket(family, SOCK_STREAM, IPPROTO_TCP);
    if (sockfd < 0)
    {
      LOG_SYSFATAL << "sockets::createNonblockingOrDie";
    }

    setNonBlockAndCloseOnExec(sockfd);
#else
    int sockfd = ::socket(family, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, IPPROTO_TCP);
    if (sockfd < 0) {
    }
#endif
    return sockfd;
}

void sockets::bindOrDie(int sockfd, const struct sockaddr *addr) {
    int ret = ::bind(sockfd, addr, static_cast<socklen_t>(sizeof(struct sockaddr_in6)));
    if (ret < 0) {
    }
}

void sockets::listenOrDie(int sockfd) {
    int ret = ::listen(sockfd, SOMAXCONN);
    if (ret < 0) {
    }
}

int sockets::accept(int sockfd, struct sockaddr_in6 *addr) {
    socklen_t addrlen = static_cast<socklen_t>(sizeof *addr);
#if VALGRIND || defined (NO_ACCEPT4)
    int connfd = ::accept(sockfd, sockaddr_cast(addr), &addrlen);
    setNonBlockAndCloseOnExec(connfd);
#else
    int connfd = ::accept4(sockfd, sockaddr_cast(addr),
                           &addrlen, SOCK_NONBLOCK | SOCK_CLOEXEC);
#endif
    if (connfd < 0) {
        int savedErrno = errno;
        switch (savedErrno) {
            case EAGAIN:
            case ECONNABORTED:
            case EINTR:
            case EPROTO: // ???
            case EPERM:
            case EMFILE: // per-process lmit of open file desctiptor ???
                // expected errors
                errno = savedErrno;
                break;
            case EBADF:
            case EFAULT:
            case EINVAL:
            case ENFILE:
            case ENOBUFS:
            case ENOMEM:
            case ENOTSOCK:
            case EOPNOTSUPP:
                // unexpected errors
                break;
            default:
                break;
        }
    }
    return connfd;
}

int sockets::connect(int sockfd, const struct sockaddr *addr) {
    return ::connect(sockfd, addr, static_cast<socklen_t>(sizeof(struct sockaddr_in6)));
}

ssize_t sockets::read(int sockfd, void *buf, size_t count) {
    return ::read(sockfd, buf, count);
}

ssize_t sockets::readv(int sockfd, const struct iovec *iov, int iovcnt) {
    return ::readv(sockfd, iov, iovcnt);
}

ssize_t sockets::write(int sockfd, const void *buf, size_t count) {
    return ::write(sockfd, buf, count);
}

void sockets::close(int sockfd) {
    if (::close(sockfd) < 0) {
    }
}

#if !(__GNUC_PREREQ(4, 6))
#pragma GCC diagnostic ignored "-Wstrict-aliasing"
#endif

