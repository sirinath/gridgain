/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

#ifndef GRID_CLIENT_ROUNDROUBIN_BALANCER_HPP_INCLUDED
#define GRID_CLIENT_ROUNDROUBIN_BALANCER_HPP_INCLUDED

#include <gridgain/loadbalancer/gridclientloadbalancer.hpp>

/**
 * Round robin balancer.
 */
class GRIDGAIN_API GridClientRoundRobinBalancer : public GridClientLoadBalancer {
public:
    /** Default constructor. */
    GridClientRoundRobinBalancer() : nodePos(0) { };

    /**
     * Gets next node for executing client command.
     *
     * @param nodes Nodes to pick from.
     * @return Next node to pick.
     */
    virtual TGridClientNodePtr balancedNode(const TGridClientNodeList& nodes);

private:
    /** Position of last node served in the list. */
    unsigned int nodePos;
};

#endif
